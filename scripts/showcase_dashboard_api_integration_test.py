#!/usr/bin/env python3
"""风采墙与数据面板真实 HTTP 验证。只允许显式指定的 hackathon_*_it_* 隔离库。
依赖本目录已有的 HTTP/报告助手；测试账号通过 HTTP 创建，历史轮次和时间边界用隔离 SQL 准备。
默认服务 18084；上传走服务配置的测试对象存储。结果不保存凭据或签名 URL。
"""
from __future__ import annotations
import argparse, base64, json, re, time, uuid
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
from pathlib import Path
from urllib.request import Request, urlopen
from urllib.parse import parse_qs, urlsplit
from registered_events_api_integration_test import Runner, mysql, token_user_id, render_report

# 本地AVFoundation生成的32x32黑色视频，避免验收依赖外部媒体地址。
VIDEO = base64.b64decode('AAAAHGZ0eXBtcDQyAAAAAWlzb21tcDQxbXA0MgAAAAFtZGF0AAAAAAAAAK4AAAA6BgUyR1ZK3FxMQz+U78URPNFDqAEAAAMAAQMAAAMAAQIAAeYACwAAAwAAAwAACAwMA4koAQ3/////gAAAAD4luCAf3gjlTP+CzB6bUNuL7ABTs2iZjkA8dTMQrttrO+cIX174LrderiMzz2ckni85HI7/BgAAKRgAP0MWrAAAABoh4QRf8Xf2Gh7661gdVgMMagAkHx7r+sL+gAAAAshtb292AAAAbG12aGQAAAAA5smGeebJhnoAAAJYAAAEsAABAAABAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAAACVHRyYWsAAABcdGtoZAAAAAHmyYZ55smGegAAAAEAAAAAAAAEsAAAAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAEAAAAAAIAAAACAAAAAAACRlZHRzAAAAHGVsc3QAAAAAAAAAAQAABLAAAAAAAAEAAAAAAcxtZGlhAAAAIG1kaGQAAAAA5smGeebJhnoAAAJYAAAEsFXEAAAAAAAxaGRscgAAAAAAAAAAdmlkZQAAAAAAAAAAAAAAAENvcmUgTWVkaWEgVmlkZW8AAAABc21pbmYAAAAUdm1oZAAAAAEAAAAAAAAAAAAAACRkaW5mAAAAHGRyZWYAAAAAAAAAAQAAAAx1cmwgAAAAAQAAATNzdGJsAAAAoXN0c2QAAAAAAAAAAQAAAJFhdmMxAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAAACAAIABIAAAASAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAGP//AAAAJ2F2Y0MBZAAL/+EADCdkAAusVlDDeBRghQEABCjuPLD9+PgAAAAACmZpZWwBAAAAAApjaHJtAAAAAAAYc3R0cwAAAAAAAAABAAAAAgAAAlgAAAAUc3RzcwAAAAAAAAABAAAAAQAAAA5zZHRwAAAAACAQAAAAHHN0c2MAAAAAAAAAAQAAAAEAAAABAAAAAQAAABxzdHN6AAAAAAAAAAAAAAACAAAAgAAAAB4AAAAYc3RjbwAAAAAAAAACAAAALAAAAKw=')

class Suite(Runner):
    def check(self, name, method, path, token=None, body=None, status=200, code=200, verify=None, group=None):
        if group is None:
            group = '风采墙' if path.startswith(('/showcase', '/file')) else '作品标签' if path.startswith(('/submission', '/review')) else '数据面板'
        actual_status, payload, elapsed = self.raw(method, path, token, body)
        actual_code = payload.get('code') if isinstance(payload, dict) else None
        data = payload.get('data') if isinstance(payload, dict) else None
        passed = actual_status == status and actual_code == code
        if passed and verify is not None:
            try: passed = bool(verify(data))
            except (KeyError, TypeError, ValueError): passed = False
        self.results.append({'group':group, 'name':name, 'method':method, 'path':path,
            'expected':f'HTTP {status} / code {code}' + ('；数据断言满足' if verify else ''),
            'actual':f'HTTP {actual_status} / code {actual_code}' + ('；数据断言通过' if passed and verify else ''),
            'durationMs':elapsed, 'passed':passed})
        if not passed: raise AssertionError(f'{name}: HTTP {actual_status} / code {actual_code} 或数据断言不匹配')
        return data


def run(args, r):
    def sql(statement): return mysql(args.mysql_container,args.database,"SET time_zone='+08:00'; SET NAMES utf8mb4; "+statement)
    def insert(statement): return int(sql(statement+'; SELECT LAST_INSERT_ID();').strip())
    nonce=uuid.uuid4().hex[:8]
    identity=int(time.time()*1000)%100000000
    users=[]
    for i in range(8):
        n=identity+i
        temp=r.check('生成测试身份','POST','/auth/test',body={'casId':f'{n:012d}','name':f'Feature-{nonce}-{i}'},group='数据准备')
        login=r.check('注册测试学生','POST','/student',body={'token':temp,'phone':f'138{n:08d}','email':f'{nonce}-{i}@example.test',
                     'campus':'中心校区' if i%2==0 else '青岛校区','major':'software','tags':[]},group='数据准备')
        users.append((token_user_id(login['token']),login['token']))
    (leader,L),(member,M),(other,O),(admin,A),(otheradmin,B),(judge,J),(judge2,K),(alone,U)=users
    e=insert(f"INSERT INTO event(name,reg_beg,reg_end,live_beg,live_end,introduction,team_min_size,team_max_size,version,create_time,update_time) VALUES('feature-{nonce}',NOW()-INTERVAL 10 DAY,NOW()-INTERVAL 6 DAY,NOW()-INTERVAL 5 DAY,NOW()+INTERVAL 5 DAY,'fixture',2,5,1,NOW(),NOW())")
    empty=insert(f"INSERT INTO event(name,reg_beg,reg_end,live_beg,live_end,version,create_time,update_time) VALUES('empty-{nonce}',NOW()-INTERVAL 10 DAY,NOW()-INTERVAL 6 DAY,NOW()-INTERVAL 5 DAY,NOW()+INTERVAL 5 DAY,1,NOW(),NOW())")
    t=insert(f"INSERT INTO track(event_id,name,desc_md,version,create_time,update_time) VALUES({e},'main','fixture',1,NOW(),NOW())")
    zero=insert(f"INSERT INTO track(event_id,name,desc_md,version,create_time,update_time) VALUES({e},'empty-track','fixture',1,NOW(),NOW())")
    foreign=insert(f"INSERT INTO track(event_id,name,desc_md,version,create_time,update_time) VALUES({empty},'foreign','fixture',1,NOW(),NOW())")
    def phase(track,name,beg,end):
        return insert(f"INSERT INTO phase(track_id,name,submit_beg,submit_end,review_beg,review_end,blind_review,mid_check,manual_pick,pass_rate,poll,submission_config,version,create_time,update_time) VALUES({track},'{name}',{beg},{end},{end}+INTERVAL 1 HOUR,NOW()+INTERVAL 4 DAY,0,0,1,0.5,0,'{{}}',1,NOW(),NOW())")
    p1=phase(t,'round1','NOW()-INTERVAL 4 DAY','NOW()-INTERVAL 3 DAY')
    p2=phase(t,'round2','NOW()-INTERVAL 2 DAY','NOW()+INTERVAL 1 DAY')
    fp=phase(foreign,'other','NOW()-INTERVAL 2 DAY','NOW()+INTERVAL 1 DAY')
    team=insert(f"INSERT INTO team(name,event_id,track_id,leader_id,size,introduction,type,status,current_phase_id,version,create_time,update_time) VALUES('team-{nonce}',{e},{t},{leader},2,'fixture',0,0,{p2},1,NOW(),NOW())")
    team2=insert(f"INSERT INTO team(name,event_id,track_id,leader_id,size,introduction,type,status,version,create_time,update_time) VALUES('other-{nonce}',{e},{t},{other},1,'fixture',0,1,1,NOW(),NOW())")
    for user,team_id in [(leader,team),(member,team),(other,team2),(alone,'NULL')]:
        sql(f"INSERT INTO registration(user_id,event_id,track_id,team_id,version,create_time,update_time) VALUES({user},{e},{t},{team_id},1,NOW(),NOW());")
    sql(f"INSERT INTO authority(user_id,type,event_id,create_time) VALUES({admin},1,{e},NOW()),({otheradmin},1,{empty},NOW()),({judge},2,{e},NOW()),({judge2},2,{e},NOW());")
    def old_submission(team_id,phase_id,who,tags):
        sid=insert(f"INSERT INTO submission(phase_id,team_id,status,version_no,submitter_id,submit_time,version,create_time,update_time,ai_tools) VALUES({phase_id},{team_id},1,1,{who},NOW(),1,NOW(),NOW(),{tags})")
        sql(f"INSERT INTO submission_version(submission_id,version_no,snapshot,submitter_id,submit_time,create_time) VALUES({sid},1,'{{}}',{who},NOW(),NOW());")
        return sid
    first=old_submission(team,p1,leader,"JSON_ARRAY('Old Tool')")
    second=old_submission(team2,p1,other,'NULL')
    base=f'/dashboard/event/{e}'
    r.check('默认无公开统计','GET',base,verify=lambda d:d['publicMetrics']==[] and d['metrics']=={})
    r.check('游客不能读取管理面板','GET',base+'/admin',status=401,code=1004)
    r.check('学生不能读取管理面板','GET',base+'/admin',L,status=403,code=1011)
    r.check('其他赛事管理员不能读取管理面板','GET',base+'/admin',B,status=403,code=1011)
    r.check('其他赛事管理员不能设置公开项','PUT',base+'/config',B,{'publicMetrics':[],'version':0},403,1011)
    r.check('不存在赛事返回404','GET','/dashboard/event/2147483647',status=404,code=3001)
    r.check('零数据管理面板','GET',f'/dashboard/event/{empty}/admin',B,verify=lambda d:d['metrics']['participantCount']==0 and all(x['submittedTeams']==0 for x in d['phases']) and d['metrics']['aiTools']['coverage'] is None)
    r.check('读取默认配置','GET',base+'/config',A,verify=lambda d:d['version']==0)
    r.check('不能公开内部统计项','PUT',base+'/config',A,{'publicMetrics':['JUDGE_PROGRESS'],'version':0},400,4000)
    r.check('拒绝重复公开项','PUT',base+'/config',A,{'publicMetrics':['TEAM_COUNT','TEAM_COUNT'],'version':0},400,4000)
    r.check('必须显式传公开项列表','PUT',base+'/config',A,{'version':0},400,4000)
    c=r.check('选择单项公开','PUT',base+'/config',A,{'publicMetrics':['TEAM_COUNT'],'version':0})
    r.check('后端只返回被选中的字段','GET',base,verify=lambda d:d['metrics']=={'teamCount':2} and 'judges' not in d and 'registration' not in d)
    r.check('配置旧版本冲突','PUT',base+'/config',A,{'publicMetrics':[],'version':0},409,4002)
    # 并发写入同一版本，只有一方成功。
    with ThreadPoolExecutor(2) as pool:
        responses=list(pool.map(lambda _:r.raw('PUT',base+'/config',A,{'publicMetrics':['PARTICIPANT_COUNT'],'version':c['version']}),range(2)))
    assert sorted(x[0] for x in responses)==[200,409]
    c=r.check('并发配置写入保持唯一版本','GET',base+'/config',A,verify=lambda d:d['version']==2)
    keys=['PARTICIPANT_COUNT','CAMPUS_DISTRIBUTION','TEAM_COUNT','TRACK_SUBMISSIONS','AI_TOOLS','TECH_STACKS']
    c=r.check('设置全部合法公开统计项','PUT',base+'/config',A,{'publicMetrics':keys,'version':c['version']})
    r.check('报名人数和校区分布同一范围','GET',base,verify=lambda d:d['metrics']['participantCount']==4 and sum(x['count'] for x in d['metrics']['campusDistribution'])==4 and len(d['metrics']['campusDistribution'])==9)
    # 校区缺失时保留总人数，归入未知。
    sql(f"DELETE FROM student WHERE user_id={alone};")
    r.check('缺失学生资料归入未知校区','GET',base,verify=lambda d:d['metrics']['campusDistribution'][-1]['count']==1 and sum(x['count'] for x in d['metrics']['campusDistribution'])==4)
    # 标签通过正式提交和补交写入，验证序列化、版本、清空、旧客户端兼容。
    sp=f'/submission/phase/{p2}/team/{team}'
    s=r.check('结构化标签首次提交','POST',sp,L,{'aiTools':[' cursor ','Cursor','Claude'],'techStacks':['vue.js','Vue','React']})
    sid=s['submissionId']
    info=r.check('标签规范化并去重','GET',f'/submission/{sid}',L,verify=lambda d:d['aiTools']==['Claude','Cursor'] and d['techStacks']==['React','Vue'])
    r.check('非法空标签被拒绝','POST',sp,L,{'version':info['version'],'aiTools':[' ']},400,4000)
    r.check('超过标签数量限制被拒绝','POST',sp,L,{'version':info['version'],'aiTools':['a']*31},400,4000)
    r.check('旧客户端缺省标签保留','POST',sp,L,{'version':info['version']})
    info=r.check('缺省后标签未丢失','GET',f'/submission/{sid}',L,verify=lambda d:d['aiTools']==['Claude','Cursor'])
    r.check('显式空数组清空技术栈','POST',sp,L,{'version':info['version'],'techStacks':[],'aiTools':None})
    info=r.check('null保留与空数组区分','GET',f'/submission/{sid}',L,verify=lambda d:d['techStacks']==[] and d['aiTools']==['Claude','Cursor'])
    r.check('历史版本保留原技术栈','GET',f'/submission/{sid}/version/1',L,verify=lambda d:d['snapshot']['techStacks']==['React','Vue'])
    r.check('存量历史快照兼容新增字段','GET',f'/submission/{first}/version/1',L,verify=lambda d:d['snapshot']['aiTools'] is None)
    r.check('跨轮次作品只计一个项目','GET',base,verify=lambda d:[x['count'] for x in d['metrics']['trackSubmissions']]==[2,0])
    r.check('词云采用最新轮次且显示覆盖率','GET',base,verify=lambda d:d['metrics']['aiTools']['totalProjects']==2 and d['metrics']['aiTools']['reportedProjects']==1 and {w['name']:w['count'] for w in d['metrics']['aiTools']['words']}=={'Claude':1,'Cursor':1} and d['metrics']['techStacks']['reportedProjects']==1 and d['metrics']['techStacks']['words']==[])
    # 评审任务、回避、异常窗口在隔离库中构造边界。
    sql(f"UPDATE phase SET submit_end=NOW()-INTERVAL 1 HOUR,review_beg=NOW()-INTERVAL 30 MINUTE,submission_config=JSON_OBJECT('website',true) WHERE phase_id={p2};")
    r.check('管理员创建评审任务','POST',f'/review/phase/{p2}/assignment',A,{'judgeIds':[judge],'submissionIds':[sid]})
    flag=r.check('评委开放补交窗口','POST',f'/review/submission/{sid}/flag',J,{'target':'在线Demo网址','description':'test unavailable demo','windowHours':1})
    r.check('补交同样保存标签','POST',f'/submission/{sid}/supplement',L,{'version':info['version'],'demoUrl':'https://example.test/demo','changeLog':'repair','aiTools':['Figma'],'techStacks':['React']})
    info=r.check('补交新版本回显标签','GET',f'/submission/{sid}',L,verify=lambda d:d['aiTools']==['Figma'])
    r.check('补交不改变旧版本快照','GET',f'/submission/{sid}/version/1',L,verify=lambda d:d['snapshot']['aiTools']==['Claude','Cursor'])
    sql(f"INSERT INTO review_assignment(phase_id,submission_id,judge_id,status,source,urge_count,version,create_time,update_time) VALUES({p1},{first},{judge},1,0,0,1,NOW(),NOW()),({p1},{second},{judge2},2,0,0,1,NOW(),NOW());")
    for status,expiry,reason in [(0,'NOW()+INTERVAL 1 HOUR','NULL'),(0,'NOW()-INTERVAL 1 HOUR','NULL'),(2,'NOW()-INTERVAL 1 HOUR',"'补交窗口已到期，系统自动关闭'")]:
        sql(f"SET NAMES utf8mb4; INSERT INTO review_flag(submission_id,phase_id,judge_id,target,description,status,supplement_end,close_reason,version,create_time,update_time) VALUES({sid},{p2},{judge},0,'fixture',{status},{expiry},{reason},1,NOW(),NOW());")
    admin_data=r.check('管理统计与代表数据一致','GET',base+'/admin',A,verify=lambda d:d['registration']=={'participants':4,'teamedParticipants':3,'unteamedParticipants':1,'teams':2,'underfilledTeams':1} and d['workItems']=={'unreplacedRecusals':1,'openFlags':1,'expiredFlags':2})
    r.check('首轮提交率与后续轮次未知分母','GET',base+'/admin',A,verify=lambda d:d['phases'][0]['submissionRate']==1 and d['phases'][1]['submissionRate'] is None and d['phases'][1]['expectedTeams'] is None and d['phases'][1]['denominatorReason']=='HISTORICAL_ELIGIBILITY_UNAVAILABLE')
    for kind,count,phase_id in [('UNTEAMED',1,None),('UNDERFILLED',1,None),('SUBMITTED',2,p1),('MISSING_SUBMISSION',0,p1),('UNASSIGNED',1,p1),('PENDING_REVIEW',1,None),('UNREPLACED_RECUSED',1,None),('OPEN_FLAGS',1,None),('EXPIRED_FLAGS',2,None)]:
        path=base+f'/admin/details?kind={kind}'+(f'&phaseId={phase_id}' if phase_id else '')
        r.check(f'明细数量匹配{kind}','GET',path,A,verify=lambda d,n=count:d['total']==n)
    r.check('管理明细不能跨赛事轮次','GET',base+f'/admin/details?kind=SUBMITTED&phaseId={fp}',A,status=404,code=3021)
    r.check('明细必须提供所需轮次','GET',base+'/admin/details?kind=SUBMITTED',A,status=400,code=4000)
    r.check('明细分页参数校验','GET',base+'/admin/details?kind=UNTEAMED&page=0',A,status=400,code=4000)
    r.check('人工补派关联回避记录','POST',f'/review/phase/{p1}/assignment',A,{'judgeIds':[judge],'submissionIds':[second]})
    r.check('补派后待分配回避计数消失','GET',base+'/admin',A,verify=lambda d:d['workItems']['unreplacedRecusals']==0)
    # 风采墙与文件生命周期。
    wall=f'/showcase/team/{team}'
    draft={'sourceSubmissionId':sid,'sourceVersionNo':info['versionNo'],'title':'Public project','summary':'Summary','introMd':'# Architecture\nA useful design.',
           'repoUrl':'https://example.test/repo','licenseType':'MIT','videoUrl':'https://example.test/video.mp4'}
    r.check('赛中不允许创建风采墙项目','PUT',wall,L,draft,403,3502)
    r.check('队员不能代替队长发布','PUT',wall,M,draft,403,1013)
    r.check('其他队长不能编辑本队项目','PUT',wall,O,draft,403,1013)
    r.check('未创建管理详情返回null','GET',wall,L,verify=lambda d:d is None)
    sql(f"UPDATE phase SET submit_end=NOW()-INTERVAL 3 HOUR,review_beg=NOW()-INTERVAL 2 HOUR,review_end=NOW()-INTERVAL 1 HOUR WHERE phase_id={p2}; UPDATE event SET live_end=NOW()-INTERVAL 30 MINUTE WHERE event_id={e};")
    r.check('作品来源不能跨队伍','PUT',wall,L,{**draft,'sourceSubmissionId':second,'sourceVersionNo':1},400,3504)
    r.check('来源版本必须存在','PUT',wall,L,{**draft,'sourceVersionNo':9999},404,3304)
    r.check('网址不能包含脚本协议','PUT',wall,L,{**draft,'repoUrl':'javascript:alert(1)'},400,4000)
    image=base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=')
    def upload(token,team_id,complete=True):
        u=r.check('申请展示文件上传','POST',f'/file/team/{team_id}/showcase',token,{'scope':'SHOWCASE','filename':'cover.png','sizeBytes':len(image)})
        with urlopen(Request(u['uploadUrl'],data=image,headers={'Content-Type':u['contentType']},method='PUT'),timeout=20) as response: assert response.status==200
        if complete:r.check('确认真实对象存储上传','POST',f"/file/{u['fileId']}/complete",token)
        return u['fileId']
    cover=upload(L,team)
    unreferenced=upload(L,team)
    pending=upload(L,team,False)
    foreign_file=upload(O,team2)
    r.check('上传完成但未公开的文件游客不可读','GET',f'/file/{cover}/url',status=401,code=1004)
    r.check('成员可以预览展示文件','GET',f'/file/{cover}/url',M,verify=lambda d:bool(d['url']))
    r.check('跨队伍文件不可绑定','PUT',wall,L,{**draft,'coverFileId':foreign_file},400,3306)
    r.check('待完成文件不可绑定','PUT',wall,L,{**draft,'coverFileId':pending},409,6005)
    d=r.check('保存展示草稿','PUT',wall,L,{**draft,'coverFileId':cover})
    wid=d['showcaseId']
    r.check('游客不能查看草稿','GET',f'/showcase/{wid}',status=404,code=3501)
    r.check('游客列表不包含草稿','GET',f'/showcase/list?eventId={e}',verify=lambda d:d['total']==0)
    r.check('草稿引用的文件禁止删除','DELETE',f'/file/{cover}',L,status=409,code=6011)
    r.check('保存使用旧版本会冲突','PUT',wall,L,{**draft,'version':99},409,4002)
    d=r.check('队长发布','POST',wall+'/publish',L,{'version':d['version']},verify=lambda d:d['status']=='PUBLISHED')
    r.check('公开详情没有内部字段','GET',f'/showcase/{wid}',verify=lambda d:d['title']==draft['title'] and not set(['operations','sourceSubmissionId','version','moderationReason','phone','casId'])&d.keys())
    r.check('公开分页与赛道筛选','GET',f'/showcase/list?eventId={e}&trackId={t}&page=1&size=1',verify=lambda d:d['total']==1 and len(d['records'])==1)
    r.check('筛选赛道不能跨赛事','GET',f'/showcase/list?eventId={e}&trackId={foreign}',status=404,code=3011)
    r.check('字面百分号不匹配所有项目','GET',f'/showcase/list?eventId={e}&keyword=%25',verify=lambda d:d['total']==0)
    file_url=r.check('公开项目引用文件允许游客读取','GET',f'/file/{cover}/url')
    with urlopen(file_url['url'],timeout=20) as response: assert response.read()==image
    r.check('同队未引用文件仍不公开','GET',f'/file/{unreferenced}/url',status=401,code=1004)
    r.check('已发布内容需撤回后编辑','PUT',wall,L,{**draft,'version':d['version']},409,3503)
    r.check('其他赛事管理员不能下架','PUT',f'/showcase/event/{empty}/{wid}/moderation',B,{'blocked':True,'reason':'test','version':d['version']},404,3501)
    d=r.check('队长撤回','POST',wall+'/withdraw',L,{'version':d['version']})
    r.check('撤回后详情不可公开访问','GET',f'/showcase/{wid}',status=404,code=3501)
    r.check('撤回后停止签发文件访问地址','GET',f'/file/{cover}/url',status=401,code=1004)
    d=r.check('撤回后可以重新发布','POST',wall+'/publish',L,{'version':d['version']})
    d=r.check('管理员下架','PUT',f'/showcase/event/{e}/{wid}/moderation',A,{'blocked':True,'reason':'content review','version':d['version']})
    r.check('下架后游客不能获取文件信息','GET',f'/file/{cover}',status=401,code=1004)
    r.check('队长不能绕过管理员下架','POST',wall+'/publish',L,{'version':d['version']},409,3503)
    r.check('队长不能编辑被下架内容','PUT',wall,L,{**draft,'version':d['version']},409,3503)
    d=r.check('解除下架不自动公开','PUT',f'/showcase/event/{e}/{wid}/moderation',A,{'blocked':False,'reason':'resolved','version':d['version']},verify=lambda d:d['status']=='WITHDRAWN')
    r.check('管理员按状态查看项目','GET',f'/showcase/event/{e}/manage/list?status=WITHDRAWN',A,verify=lambda d:d['total']==1)
    d=r.check('解除文件引用及清空可选字段','PUT',wall,L,{**draft,'summary':None,'coverFileId':None,'version':d['version']},verify=lambda d:d['coverFileId'] is None and d['summary'] is None and d['files']==[])
    for fid,token in [(cover,L),(unreferenced,L),(pending,L),(foreign_file,O)]:
        r.check('测试文件解除引用后可删除','DELETE',f'/file/{fid}',token)
    d=r.check('保存缺少发布必填项的草稿','PUT',wall,L,{'sourceSubmissionId':sid,'sourceVersionNo':info['versionNo'],'title':'incomplete','version':d['version']})
    r.check('发布前校验完整展示信息','POST',wall+'/publish',L,{'version':d['version']},400,4000)
    r.check('操作日志仅管理详情提供','GET',wall,A,verify=lambda d:len(d['operations'])>=8)
    # 独立验证上传视频与所选参赛版本视频复用的权限。
    def upload_video():
        u=r.check('申请展示视频上传','POST',f'/file/team/{team}/showcase',L,{'scope':'SHOWCASE','filename':'demo.mp4','sizeBytes':len(VIDEO)})
        with urlopen(Request(u['uploadUrl'],data=VIDEO,headers={'Content-Type':u['contentType']},method='PUT'),timeout=20) as response: assert response.status==200
        r.check('视频上传完成校验','POST',f"/file/{u['fileId']}/complete",L)
        return u['fileId']
    video=upload_video()
    other_video=upload_video()
    r.check('视频不能用作封面','PUT',wall,L,{**draft,'coverFileId':video,'version':d['version']},400,6002)
    r.check('视频文件和链接不能同时设置','PUT',wall,L,{**draft,'videoFileId':video,'version':d['version']},400,4000)
    d=r.check('保存本队展示视频','PUT',wall,L,{**draft,'videoFileId':video,'videoUrl':None,'version':d['version']})
    d=r.check('展示视频发布','POST',wall+'/publish',L,{'version':d['version']})
    vu=r.check('公开视频URL有效期为五分钟','GET',f'/file/{video}/url',verify=lambda d:parse_qs(urlsplit(d['url']).query)['X-Amz-Expires']==['300'])
    with urlopen(vu['url'],timeout=20) as response: assert response.read()==VIDEO
    r.check('同队未引用的视频保持私有','GET',f'/file/{other_video}/url',status=401,code=1004)
    d=r.check('撤回展示视频','POST',wall+'/withdraw',L,{'version':d['version']})
    # 将已上传文件设为隔离历史参赛视频，构造来源版本关系，不修改生产资料。
    sql(f"UPDATE file_object SET scope=11,phase_id={p2} WHERE file_id IN({video},{other_video}); UPDATE submission_version SET snapshot=JSON_SET(snapshot,'$.videoFileId',{video}) WHERE submission_id={sid} AND version_no={info['versionNo']};")
    r.check('不能公开所选版本以外的参赛视频','PUT',wall,L,{**draft,'videoFileId':other_video,'videoUrl':None,'version':d['version']},400,6006)
    d=r.check('复用所选版本的参赛视频','PUT',wall,L,{**draft,'videoFileId':video,'videoUrl':None,'version':d['version']})
    d=r.check('复用参赛视频发布','POST',wall+'/publish',L,{'version':d['version']})
    r.check('复用参赛视频公开签名仍为五分钟','GET',f'/file/{video}/url',verify=lambda d:parse_qs(urlsplit(d['url']).query)['X-Amz-Expires']==['300'])
    sql(f"UPDATE submission SET intro_md='later submission content' WHERE submission_id={sid};")
    r.check('原作品变更不改变公开展示正文','GET',f'/showcase/{wid}',verify=lambda d:d['introMd']==draft['introMd'])
    d=r.check('撤回复用参赛视频','POST',wall+'/withdraw',L,{'version':d['version']})
    r.check('撤回后参赛视频恢复私有','GET',f'/file/{video}/url',status=401,code=1004)
    d=r.check('清除视频引用','PUT',wall,L,{**draft,'version':d['version']})
    for fid in [video,other_video]:r.check('清理视频测试文件','DELETE',f'/file/{fid}',L)
    for metric,field in zip(keys,['participantCount','campusDistribution','teamCount','trackSubmissions','aiTools','techStacks']):
        c=r.check('单独配置公开项'+metric,'PUT',base+'/config',A,{'publicMetrics':[metric],'version':c['version']})
        r.check('公开响应仅包含'+field,'GET',base,verify=lambda d,f=field:set(d['metrics'])=={f})
    c=r.check('取消全部公开统计项','PUT',base+'/config',A,{'publicMetrics':[],'version':c['version']})
    r.check('取消后公开响应不残留任何统计字段','GET',base,verify=lambda d:d['metrics']=={} and d['publicMetrics']==[])
    return {'eventId':e,'emptyEventId':empty}

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base-url',default='http://127.0.0.1:18084')
    parser.add_argument('--mysql-container',default='mysql')
    parser.add_argument('--database',required=True)
    parser.add_argument('--result',default='reports/showcase-dashboard-api-results.json')
    parser.add_argument('--report',default='reports/showcase-dashboard-api-report.html')
    args=parser.parse_args()
    if not re.fullmatch(r'hackathon_[a-z0-9_]*_it_[a-z0-9_]+',args.database): parser.error('必须使用独立的 hackathon_*_it_* 数据库')
    r=Suite(args.base_url); started=datetime.now(timezone.utc).isoformat(); error=None
    try: run(args,r)
    except Exception as e:
        error=type(e).__name__
        print('FAILED:',str(e)[:500])
        if not r.results or r.results[-1]['passed']:
            r.results.append({'group':'执行检查','name':'文件内容或并发断言','method':'CHECK','path':'local',
                'expected':'所有断言通过','actual':error,'durationMs':0,'passed':False})
    report={'baseUrl':args.base_url,'database':args.database,'startedAt':started,'finishedAt':datetime.now(timezone.utc).isoformat(),
            'results':r.results,'total':len(r.results),'passed':sum(x['passed'] for x in r.results),'failed':sum(not x['passed'] for x in r.results),'executionError':error}
    Path(args.result).parent.mkdir(parents=True,exist_ok=True)
    Path(args.result).write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    render_report(report,Path(args.report))
    p=Path(args.report); html=p.read_text().replace('已报名赛事列表 API 验证报告','风采墙与数据面板 API 验证报告').replace('外部 Python 客户端通过真实 Spring Boot HTTP 接口验证当前学生的报名筛选、分页排序、身份隔离与参数校验。','外部 Python 客户端验证风采墙发布与文件权限、公开统计项配置、管理面板、结构化标签和版本兼容。').replace('学生通过 HTTP 注册；赛事与报名关系为隔离夹具。','学生通过 HTTP 注册；赛事、历史轮次及异常边界由隔离 SQL 准备；展示文件使用测试对象存储上传并核对内容。')
    p.write_text(html)
    print(f"passed={report['passed']} failed={report['failed']} total={report['total']} error={error}")
    if error or report['failed']: raise SystemExit(1)
if __name__=='__main__': main()
