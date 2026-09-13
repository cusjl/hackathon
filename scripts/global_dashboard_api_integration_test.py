#!/usr/bin/env python3
"""全局公开面板与原赛事面板兼容验收，使用专用空数据隔离库，不访问对象存储。"""
from __future__ import annotations

import argparse
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
import json
from pathlib import Path
import re
import time
import uuid

from registered_events_api_integration_test import mysql, render_report, token_user_id
from showcase_dashboard_api_integration_test import Suite

METRICS = {
    'PARTICIPANT_COUNT': 'participantCount', 'CAMPUS_DISTRIBUTION': 'campusDistribution',
    'TEAM_COUNT': 'teamCount', 'SUBMISSION_COUNT': 'submissionCount',
    'TRACK_SUBMISSIONS': 'trackSubmissions', 'AI_TOOLS': 'aiTools', 'TECH_STACKS': 'techStacks',
}


def run(args, r):
    def sql(statement):
        return mysql(args.mysql_container, args.database, "SET time_zone='+08:00'; SET NAMES utf8mb4; " + statement)

    def insert(statement):
        return int(sql(statement + '; SELECT LAST_INSERT_ID();').strip())

    # 全局数值测试要求独占隔离库；发现已有业务数据立即退出，绝不清空。
    for table in ['registration', 'team', 'submission', 'track']:
        if int(sql(f'SELECT COUNT(*) FROM {table};')):
            raise ValueError('请使用无报名、队伍、作品、赛道数据的专用隔离库')
    nonce = uuid.uuid4().hex[:8]
    users = []
    seed = int(time.time() * 1000) % 100000000
    for i in range(5):
        n = seed + i
        token = r.check('创建测试身份', 'POST', '/auth/test', body={'casId': f'{n:012d}', 'name': f'Global-{nonce}-{i}'})
        login = r.check('注册测试选手', 'POST', '/student', body={
            'token': token, 'phone': f'138{n:08d}', 'email': f'{nonce}-{i}@example.test',
            'campus': '青岛校区' if i == 1 else '中心校区', 'major': 'software', 'tags': [],
        })
        users.append((token_user_id(login['token']), login['token']))
    (leader, L), (other, O), (alone, U), (super_id, S), (admin_id, A) = users
    sql(f'INSERT INTO authority(user_id,type,event_id,create_time) VALUES({super_id},0,NULL,NOW());')
    r.check('全局默认不公开且无赛事字段', 'GET', '/dashboard', verify=lambda d: set(d) == {'generatedAt', 'publicMetrics', 'metrics'} and d['metrics'] == {} and d['publicMetrics'] == [])
    c = r.check('全局配置初始版本', 'GET', '/dashboard/config', S, verify=lambda d: d == {'publicMetrics': [], 'version': 0, 'updateTime': None})
    for method, body in [('GET', None), ('PUT', {'publicMetrics': [], 'version': 0})]:
        r.check('游客不能管理全局配置', method, '/dashboard/config', body=body, status=401, code=1004)
        r.check('普通选手不能管理全局配置', method, '/dashboard/config', L, body, status=403, code=1006)
    for invalid in [{}, {'publicMetrics': None, 'version': 0}, {'publicMetrics': ['UNKNOWN'], 'version': 0},
                    {'publicMetrics': ['TEAM_COUNT'] * 2, 'version': 0}, {'publicMetrics': [], 'version': -1}]:
        r.check('拒绝非法公开配置', 'PUT', '/dashboard/config', S, invalid, status=400, code=4000)
    c = r.check('空库公开七项', 'PUT', '/dashboard/config', S, {'publicMetrics': list(METRICS), 'version': c['version']})
    r.check('空库零值和空分布', 'GET', '/dashboard', verify=lambda d: set(d['metrics']) == set(METRICS.values()) and all(d['metrics'][k] == 0 for k in ['participantCount', 'teamCount', 'submissionCount']) and d['metrics']['trackSubmissions'] == [] and sum(x['count'] for x in d['metrics']['campusDistribution']) == 0 and d['metrics']['aiTools']['coverage'] is None)

    def event(name):
        return insert(f"INSERT INTO event(name,reg_beg,reg_end,live_beg,live_end,version,create_time,update_time) VALUES('{name}-{nonce}',NOW()-INTERVAL 10 DAY,NOW()-INTERVAL 6 DAY,NOW()-INTERVAL 5 DAY,NOW()+INTERVAL 5 DAY,1,NOW(),NOW())")

    def track(e):
        return insert(f"INSERT INTO track(event_id,name,desc_md,version,create_time,update_time) VALUES({e},'same-name','test',1,NOW(),NOW())")

    def phase(t, days):
        return insert(f"INSERT INTO phase(track_id,name,submit_beg,submit_end,review_beg,review_end,blind_review,mid_check,manual_pick,pass_rate,poll,submission_config,version,create_time,update_time) VALUES({t},'round-{days}',NOW()-INTERVAL {days} DAY,NOW()+INTERVAL 1 DAY,NOW()+INTERVAL 2 DAY,NOW()+INTERVAL 3 DAY,0,0,1,0.5,0,'{{}}',1,NOW(),NOW())")

    def team(e, t, who, phase_id):
        return insert(f"INSERT INTO team(name,event_id,track_id,leader_id,size,type,status,current_phase_id,version,create_time,update_time) VALUES('team-{nonce}-{t}-{who}',{e},{t},{who},1,0,0,{phase_id},1,NOW(),NOW())")

    e1, e2, e3 = event('first'), event('second'), event('empty')
    t1, t2, t3 = track(e1), track(e2), track(e3)
    p1, p2, p3 = phase(t1, 4), phase(t1, 2), phase(t2, 2)
    a, b, unsubmitted = team(e1, t1, leader, p2), team(e2, t2, leader, p3), team(e1, t1, other, p2)
    for who, e, t, tm in [(leader, e1, t1, a), (leader, e2, t2, b), (other, e1, t1, unsubmitted), (alone, e1, t1, 'NULL')]:
        sql(f'INSERT INTO registration(user_id,event_id,track_id,team_id,version,create_time,update_time) VALUES({who},{e},{t},{tm},1,NOW(),NOW());')
    sql(f'INSERT INTO authority(user_id,type,event_id,create_time) VALUES({admin_id},1,{e1},NOW());')
    for method, body in [('GET', None), ('PUT', {'publicMetrics': [], 'version': c['version']})]:
        r.check('赛事管理员不能管理全局配置', method, '/dashboard/config', A, body, status=403, code=1006)

    # 原赛事配置仍可使用，且不会修改全局配置。
    totals = ['PARTICIPANT_COUNT', 'TEAM_COUNT', 'SUBMISSION_COUNT']
    event_base = f'/dashboard/event/{e1}'
    ec = r.check('原赛事配置默认值', 'GET', event_base + '/config', A, verify=lambda d: d['eventId'] == e1 and d['version'] == 0)
    ec = r.check('赛事管理员保存原公开配置', 'PUT', event_base + '/config', A, {'publicMetrics': totals, 'version': ec['version']})
    r.check('赛事配置不影响全局配置', 'GET', '/dashboard/config', S, verify=lambda d: d['version'] == c['version'] and set(d['publicMetrics']) == set(METRICS))
    r.check('其他赛事配置仍受权限限制', 'GET', f'/dashboard/event/{e2}/config', A, status=403, code=1011)
    old = insert(f"INSERT INTO submission(phase_id,team_id,status,version_no,submitter_id,submit_time,version,create_time,update_time,ai_tools) VALUES({p1},{a},1,1,{leader},NOW(),1,NOW(),NOW(),JSON_ARRAY('Old Tool'))")
    sql(f"INSERT INTO submission_version(submission_id,version_no,snapshot,submitter_id,submit_time,create_time) VALUES({old},1,'{{}}',{leader},NOW(),NOW());")
    created = r.check('同队新轮次提交', 'POST', f'/submission/phase/{p2}/team/{a}', L, {'aiTools': ['Cursor'], 'techStacks': ['React']})
    detail = r.check('读取当前版本', 'GET', f"/submission/{created['submissionId']}", L)
    r.check('同队修改作品产生新版本', 'POST', f'/submission/phase/{p2}/team/{a}', L, {'version': detail['version'], 'aiTools': ['Cursor'], 'techStacks': ['React']})
    r.check('确认已生成第二版', 'GET', f"/submission/{created['submissionId']}", L, verify=lambda d: d['versionNo'] == 2)
    r.check('同选手另一赛事提交作品', 'POST', f'/submission/phase/{p3}/team/{b}', L, {'aiTools': ['Claude'], 'techStacks': ['Vue']})
    r.check('全局选手3人队伍3支作品2件', 'GET', '/dashboard', verify=lambda d: all(d['metrics'][k] == v for k, v in {'participantCount': 3, 'teamCount': 3, 'submissionCount': 2}.items()) and not {'eventId', 'eventName', 'publicConfig', 'registration', 'judges'} & d.keys())
    r.check('跨赛事校区统计同样去重', 'GET', '/dashboard', verify=lambda d: sum(x['count'] for x in d['metrics']['campusDistribution']) == 3 and d['metrics']['campusDistribution'][0]['count'] == 2)
    r.check('同名赛道分别保留所属赛事', 'GET', '/dashboard', verify=lambda d: [(x['eventId'], x['trackId'], x['count']) for x in d['metrics']['trackSubmissions']] == [(e1, t1, 1), (e2, t2, 1), (e3, t3, 0)] and all(x['eventName'] for x in d['metrics']['trackSubmissions']))
    r.check('全局词云只使用每队最新轮次版本', 'GET', '/dashboard', verify=lambda d: d['metrics']['aiTools']['totalProjects'] == 2 and d['metrics']['aiTools']['reportedProjects'] == 2 and {x['name']: x['count'] for x in d['metrics']['aiTools']['words']} == {'Cursor': 1, 'Claude': 1} and {x['name']: x['count'] for x in d['metrics']['techStacks']['words']} == {'React': 1, 'Vue': 1})
    sql(f'DELETE FROM student WHERE user_id={alone};')
    r.check('缺失资料不丢失参赛选手', 'GET', '/dashboard', verify=lambda d: d['metrics']['participantCount'] == 3 and d['metrics']['campusDistribution'][-1]['count'] == 1 and sum(x['count'] for x in d['metrics']['campusDistribution']) == 3)
    r.check('原公开接口仍只返回本赛事统计', 'GET', event_base, verify=lambda d: d['eventId'] == e1 and d['metrics'] == {'participantCount': 3, 'teamCount': 2, 'submissionCount': 1})
    r.check('另一赛事未配置仍不公开', 'GET', f'/dashboard/event/{e2}', verify=lambda d: d['metrics'] == {})
    for e, token, expected in [(e1, A, (3, 2, 1)), (e2, S, (1, 1, 1)), (e3, S, (0, 0, 0))]:
        r.check('Admin继续按赛事统计', 'GET', f'/dashboard/event/{e}/admin', token, verify=lambda d, e=e, n=expected: d['eventId'] == e and tuple(d['metrics'][k] for k in ['participantCount', 'teamCount', 'submissionCount']) == n and d['publicConfig']['eventId'] == e and all('eventId' not in x for x in d['metrics']['trackSubmissions']))
    r.check('Admin明细仍可使用', 'GET', event_base + '/admin/details?kind=SUBMITTED&phaseId=' + str(p2), A, verify=lambda d: d['total'] == 1)
    r.check('Admin不能跨赛事查看', 'GET', f'/dashboard/event/{e2}/admin', A, status=403, code=1011)
    r.check('游客不能查看Admin', 'GET', event_base + '/admin', status=401, code=1004)

    for metric, field in METRICS.items():
        c = r.check('单独公开全局' + metric, 'PUT', '/dashboard/config', S, {'publicMetrics': [metric], 'version': c['version']})
        r.check('仅返回全局所选字段' + field, 'GET', '/dashboard', verify=lambda d, field=field: set(d['metrics']) == {field})
    r.check('全局配置不改变原赛事配置', 'GET', event_base + '/config', A, verify=lambda d: d['version'] == ec['version'] and set(d['publicMetrics']) == set(totals))
    with ThreadPoolExecutor(2) as pool:
        responses = list(pool.map(lambda _: r.raw('PUT', '/dashboard/config', S, {'publicMetrics': totals, 'version': c['version']}), range(2)))
    assert sorted((x[0], x[1]['code']) for x in responses) == [(200, 200), (409, 4002)]
    c = r.check('并发配置只有一次更新成功', 'GET', '/dashboard/config', S, verify=lambda d: d['version'] == c['version'] + 1)
    r.check('旧版本写入被拒绝', 'PUT', '/dashboard/config', S, {'publicMetrics': [], 'version': 0}, status=409, code=4002)
    c = r.check('关闭全局公开统计', 'PUT', '/dashboard/config', S, {'publicMetrics': [], 'version': c['version']})
    r.check('关闭全局后不返回数据', 'GET', '/dashboard', verify=lambda d: d['publicMetrics'] == [] and d['metrics'] == {})
    r.check('关闭全局不影响赛事公开统计', 'GET', event_base, verify=lambda d: d['metrics']['submissionCount'] == 1)
    ec = r.check('关闭原赛事公开统计', 'PUT', event_base + '/config', A, {'publicMetrics': [], 'version': ec['version']})
    r.check('原赛事仍支持清空公开配置', 'GET', event_base, verify=lambda d: d['metrics'] == {})
    c = r.check('仅全局重新公开', 'PUT', '/dashboard/config', S, {'publicMetrics': totals, 'version': c['version']})
    r.check('赛事关闭后全局仍可独立公开', 'GET', '/dashboard', verify=lambda d: d['metrics'] == {'participantCount': 3, 'teamCount': 3, 'submissionCount': 2})
    r.check('全局重新公开不会打开原赛事', 'GET', event_base, verify=lambda d: d['metrics'] == {})


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base-url', default='http://127.0.0.1:18084')
    parser.add_argument('--mysql-container', default='mysql')
    parser.add_argument('--database', required=True)
    parser.add_argument('--result', default='reports/global-dashboard-api-results.json')
    parser.add_argument('--report', default='reports/global-dashboard-api-report.html')
    args = parser.parse_args()
    if not re.fullmatch(r'hackathon_[a-z0-9_]*_it_[a-z0-9_]+', args.database):
        parser.error('必须使用独立的 hackathon_*_it_* 数据库')
    r = Suite(args.base_url)
    started = datetime.now(timezone.utc).isoformat()
    error = None
    try:
        run(args, r)
    except Exception as exc:
        error = type(exc).__name__
        print('FAILED:', str(exc)[:300])
        if not r.results or r.results[-1]['passed']:
            r.results.append({'group': '执行检查', 'name': '数据准备或并发断言', 'method': 'CHECK', 'path': 'local', 'expected': '执行成功', 'actual': error, 'durationMs': 0, 'passed': False})
    report = {'baseUrl': args.base_url, 'database': args.database, 'startedAt': started,
              'finishedAt': datetime.now(timezone.utc).isoformat(), 'results': r.results,
              'total': len(r.results), 'passed': sum(x['passed'] for x in r.results),
              'failed': sum(not x['passed'] for x in r.results), 'executionError': error}
    for path in [args.result, args.report]:
        Path(path).parent.mkdir(parents=True, exist_ok=True)
    Path(args.result).write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
    render_report(report, Path(args.report))
    p = Path(args.report)
    p.write_text(p.read_text().replace('已报名赛事列表 API 验证报告', '全局与赛事数据面板 API 验证报告').replace(
        '外部 Python 客户端通过真实 Spring Boot HTTP 接口验证当前学生的报名筛选、分页排序、身份隔离与参数校验。',
        '验证全局选手去重、队伍和作品总数、赛道与词云、全局和赛事配置独立性，以及原有赛事接口兼容性。').replace(
        '学生通过 HTTP 注册；赛事与报名关系为隔离夹具。',
        '学生注册与新作品提交使用 HTTP；历史记录由专用隔离 SQL 准备，不访问对象存储。'))
    print(f"passed={report['passed']} failed={report['failed']} total={report['total']}")
    if error or report['failed']:
        raise SystemExit(1)


if __name__ == '__main__':
    main()
