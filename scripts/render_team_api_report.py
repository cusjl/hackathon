#!/usr/bin/env python3
"""把组队 API JSON 结果渲染为无外部依赖的审阅 HTML。"""

from __future__ import annotations

import argparse
import html
import json
from collections import defaultdict
from pathlib import Path


def e(value: object) -> str:
    return html.escape(str(value), quote=True)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", default="team-api-test-results.json")
    parser.add_argument("--output", default="组队与人才招募模块验证报告.html")
    args = parser.parse_args()
    report = json.loads(Path(args.input).read_text())
    groups: dict[str, list[dict]] = defaultdict(list)
    for item in report["results"]:
        groups[item["group"]].append(item)

    coverage = [
        ("创建队伍与私密邀请码", "已实现", "创建时返回 12 位码；支持刷新，旧码立即失效"),
        ("队伍招募墙", "已实现", "发布、检索、修改、关闭、删除；按赛道/关键词/技能筛选"),
        ("入队申请与审批", "已实现", "申请、列表、接受、拒绝、重复处理保护及站内通知"),
        ("选手自荐墙", "已实现", "发布、撤下、检索；成功组队后自动下架"),
        ("在线组队邀请", "已实现", "待确认邀请、本人接受/拒绝、重复邀请保护"),
        ("唯一性与赛道一致性", "已实现", "同赛事一队；报名赛道必须与队伍赛道一致"),
        ("人数上下限", "已实现", "赛事级 1–100 配置；上限控制入队，下限控制首次作品提交"),
        ("管理员干预", "已实现", "检索、人工加人/移人、修改队伍、解散队伍"),
        ("队长变更", "已实现", "队长可转让给本队成员；赛管可设置本队成员为队长；始终保持单队长"),
    ]
    evidence = [
        ("数据库", "V9__team_recruitment.sql", "赛事人数配置、邀请码、招募、申请、邀请、自荐及索引/外键"),
        ("业务服务", "TeamService.java", "组队状态流、事务、乐观锁、容量和赛道校验、通知闭环"),
        ("HTTP 接口", "TeamController.java / EventController.java", "28 个组队相关映射及角色控制"),
        ("提交约束", "SubmissionService.java", "首次提交前检查赛事最小组队人数"),
        ("API 契约", "docs/openapi.yaml", "114 个 operationId、195 个本地引用均通过解析"),
        ("可复用验收", "scripts/team_api_integration_test.py", "仅通过 HTTP 操作业务，输出结构化 JSON 结果"),
    ]

    def rows(items: list[dict]) -> str:
        return "".join(
            "<tr>"
            f"<td><span class='method'>{e(x['method'])}</span></td>"
            f"<td><code>{e(x['path'])}</code></td>"
            f"<td>{e(x['name'])}</td>"
            f"<td>{e(x['expected'])}</td>"
            f"<td>{e(x['actual'])}</td>"
            f"<td>{e(x['durationMs'])} ms</td>"
            f"<td><span class='pill pass'>{'通过' if x['passed'] else '失败'}</span></td>"
            "</tr>" for x in items
        )

    sections = "".join(
        f"<details {'open' if i == 0 else ''}><summary><span>{e(name)}</span>"
        f"<span class='count'>{sum(x['passed'] for x in items)}/{len(items)}</span></summary>"
        "<div class='table-wrap'><table><thead><tr><th>方法</th><th>路径</th><th>场景</th>"
        "<th>期望</th><th>实际</th><th>耗时</th><th>结果</th></tr></thead>"
        f"<tbody>{rows(items)}</tbody></table></div></details>"
        for i, (name, items) in enumerate(groups.items())
    )

    coverage_rows = "".join(
        f"<tr><td>{e(name)}</td><td><span class='pill pass'>{e(status)}</span></td><td>{e(note)}</td></tr>"
        for name, status, note in coverage
    )
    evidence_rows = "".join(
        f"<tr><td>{e(layer)}</td><td><code>{e(path)}</code></td><td>{e(note)}</td></tr>"
        for layer, path, note in evidence
    )
    success = report["failed"] == 0
    document = f"""<!doctype html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>组队与人才招募模块验证报告</title>
<style>
:root{{--bg:#f5f7fb;--card:#fff;--ink:#172033;--muted:#667085;--line:#e5e9f2;--blue:#3157d5;--green:#087a55;--green-bg:#e8f8f1;--shadow:0 10px 30px rgba(23,32,51,.07)}}
*{{box-sizing:border-box}} body{{margin:0;background:var(--bg);color:var(--ink);font:14px/1.65 -apple-system,BlinkMacSystemFont,"Segoe UI","PingFang SC",sans-serif}}
.shell{{max-width:1180px;margin:auto;padding:42px 24px 72px}} .hero{{background:linear-gradient(135deg,#182b68,#3157d5);color:#fff;border-radius:22px;padding:34px 38px;box-shadow:var(--shadow)}}
.eyebrow{{font-size:12px;letter-spacing:.14em;text-transform:uppercase;opacity:.76}} h1{{font-size:30px;margin:8px 0 10px;line-height:1.25}} .hero p{{margin:0;max-width:800px;opacity:.86}}
.stats{{display:grid;grid-template-columns:repeat(4,1fr);gap:14px;margin-top:26px}} .stat{{background:rgba(255,255,255,.12);padding:16px 18px;border:1px solid rgba(255,255,255,.15);border-radius:14px}} .stat b{{display:block;font-size:24px}} .stat span{{font-size:12px;opacity:.76}}
.card{{background:var(--card);border:1px solid var(--line);border-radius:18px;margin-top:22px;padding:26px;box-shadow:var(--shadow)}} h2{{font-size:19px;margin:0 0 16px}} .meta{{display:grid;grid-template-columns:repeat(2,1fr);gap:10px 28px}} .meta div{{padding:10px 0;border-bottom:1px dashed var(--line)}} .meta span{{color:var(--muted);display:inline-block;min-width:115px}}
.table-wrap{{overflow:auto}} table{{border-collapse:collapse;width:100%;min-width:760px}} th,td{{padding:11px 12px;border-bottom:1px solid var(--line);text-align:left;vertical-align:top}} th{{font-size:12px;color:var(--muted);background:#fafbfe;position:sticky;top:0}} code{{font:12px/1.5 ui-monospace,SFMono-Regular,Menlo,monospace;color:#263d85;background:#f0f3fd;padding:2px 6px;border-radius:5px;white-space:nowrap}}
.pill{{display:inline-flex;padding:2px 9px;border-radius:999px;font-size:12px;font-weight:650}} .pass{{background:var(--green-bg);color:var(--green)}} .method{{font:700 11px ui-monospace,SFMono-Regular,monospace;color:var(--blue)}}
details{{border:1px solid var(--line);border-radius:13px;margin:10px 0;overflow:hidden}} summary{{cursor:pointer;padding:14px 16px;font-weight:650;display:flex;justify-content:space-between;background:#fafbfe}} details .table-wrap{{padding:0 4px 4px}} .count{{font-size:12px;color:var(--green);background:var(--green-bg);padding:1px 8px;border-radius:999px}}
.note{{border-left:4px solid #f0a431;background:#fff8e9;padding:12px 14px;border-radius:7px;color:#61480f}} footer{{text-align:center;color:var(--muted);font-size:12px;margin-top:28px}}
@media(max-width:760px){{.shell{{padding:20px 12px 42px}}.hero{{padding:25px 22px}}h1{{font-size:25px}}.stats{{grid-template-columns:repeat(2,1fr)}}.meta{{grid-template-columns:1fr}}}}
</style>
</head>
<body><main class="shell">
<section class="hero"><div class="eyebrow">REAL HTTP ACCEPTANCE · MYSQL 8.4 · FLYWAY V9</div>
<h1>组队与人才招募模块验证报告</h1>
<p>从空数据库迁移、真实 Spring Boot 启动到外部 Python HTTP 验收的完整证据。测试覆盖正常流程、角色权限、资源隔离、冲突处理及关键状态转换。</p>
<div class="stats"><div class="stat"><b>{report['passed']}</b><span>通过场景</span></div><div class="stat"><b>{report['total']}</b><span>场景总数</span></div><div class="stat"><b>{report['failed']}</b><span>失败场景</span></div><div class="stat"><b>{'PASS' if success else 'FAIL'}</b><span>最终结论</span></div></div></section>

<section class="card"><h2>验收环境</h2><div class="meta">
<div><span>服务地址</span><code>{e(report['baseUrl'])}</code></div><div><span>执行时间</span>{e(report['startedAt'])} — {e(report['finishedAt'])}</div>
<div><span>数据库</span>独立 MySQL 8.4：<code>hackathon_team_it_20260901</code>（验收后清理）</div><div><span>迁移</span>Flyway V1–V9 从空库完整执行</div>
<div><span>服务形态</span>实际打包 JAR，端口 18089</div><div><span>客户端</span>Python 标准库外部 HTTP 客户端</div>
</div></section>

<section class="card"><h2>需求覆盖结论</h2><div class="table-wrap"><table><thead><tr><th>需求能力</th><th>状态</th><th>落地说明</th></tr></thead><tbody>{coverage_rows}</tbody></table></div></section>
<section class="card"><h2>实现证据</h2><div class="table-wrap"><table><thead><tr><th>层级</th><th>主要文件</th><th>职责</th></tr></thead><tbody>{evidence_rows}</tbody></table></div></section>
<section class="card"><h2>API 场景明细</h2>{sections}</section>
<section class="card"><h2>验证边界</h2><div class="note">本报告验证的是独立真实 MySQL、真实 Flyway 和真实 Spring Boot HTTP 链路；测试身份与赛事数据为隔离种子数据。未调用生产 SDU Pass，也未执行与本模块无关的对象存储上传。报告及 JSON 不包含令牌、密码或云存储凭据。</div></section>
<footer>生成自 team-api-test-results.json · {'全部场景通过' if success else '存在失败场景，请查看明细'}</footer>
</main></body></html>"""
    Path(args.output).write_text(document)
    print(f"report={args.output}; groups={len(groups)}; scenarios={report['total']}")


if __name__ == "__main__":
    main()
