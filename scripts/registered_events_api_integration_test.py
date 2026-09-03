#!/usr/bin/env python3
"""当前学生已报名赛事列表的真实 HTTP 验收。

脚本只针对调用方指定的隔离 MySQL schema 运行。学生身份由公开 HTTP 接口创建；
赛事、赛道和报名关系是为分页/隔离断言准备的最小化测试夹具。结果会保留为 JSON
和不含凭据的 HTML 报告，便于重复执行与审阅。
"""

from __future__ import annotations

import argparse
import base64
import html
import json
import re
import subprocess
import time
import urllib.error
import urllib.request
import uuid
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Callable


class Runner:
    def __init__(self, base_url: str) -> None:
        self.base_url = base_url.rstrip("/")
        self.results: list[dict[str, Any]] = []

    def raw(self, method: str, path: str, token: str | None = None,
            body: Any = None) -> tuple[int, Any, float]:
        content = None if body is None else json.dumps(body, ensure_ascii=False).encode()
        headers = {"Accept": "application/json"}
        if content is not None:
            headers["Content-Type"] = "application/json"
        if token:
            headers["Authorization"] = f"Bearer {token}"
        request = urllib.request.Request(self.base_url + path, data=content, headers=headers, method=method)
        started = time.perf_counter()
        try:
            with urllib.request.urlopen(request, timeout=10) as response:
                response_status, raw = response.status, response.read()
        except urllib.error.HTTPError as error:
            response_status, raw = error.code, error.read()
        elapsed = round((time.perf_counter() - started) * 1000, 1)
        try:
            payload = json.loads(raw.decode()) if raw else None
        except json.JSONDecodeError:
            payload = {"raw": raw.decode(errors="replace")}
        return response_status, payload, elapsed

    def case(self, group: str, name: str, method: str, path: str,
             token: str | None = None, body: Any = None,
             status: int = 200, code: int = 200,
             check: Callable[[dict[str, Any]], bool] | None = None) -> dict[str, Any]:
        actual_status, payload, elapsed = self.raw(method, path, token, body)
        actual_code = payload.get("code") if isinstance(payload, dict) else None
        passed = actual_status == status and actual_code == code
        if passed and check is not None:
            try:
                passed = check(payload)
            except (KeyError, TypeError, ValueError):
                passed = False
        self.results.append({
            "group": group,
            "name": name,
            "method": method,
            "path": path,
            "expected": f"HTTP {status} / code {code}",
            "actual": f"HTTP {actual_status} / code {actual_code}",
            "durationMs": elapsed,
            "passed": passed,
            "message": payload.get("msg") if isinstance(payload, dict) else None,
        })
        if not passed:
            raise AssertionError(f"{name}: {self.results[-1]} payload={payload}")
        return payload


def mysql(container: str, database: str, statement: str) -> str:
    if not re.fullmatch(r"[A-Za-z0-9_]+", database):
        raise ValueError("database must contain only letters, digits, and underscores")
    command = 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --batch --skip-column-names -uroot ' + database
    completed = subprocess.run(
        ["docker", "exec", "-i", container, "sh", "-c", command],
        input=statement.encode(), stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False,
    )
    if completed.returncode != 0:
        raise RuntimeError(completed.stderr.decode(errors="replace"))
    return completed.stdout.decode()


def token_user_id(token: str) -> int:
    payload = token.split(".")[1]
    payload += "=" * (-len(payload) % 4)
    return int(json.loads(base64.urlsafe_b64decode(payload)) ["sub"])


def student_payload(phone: str, email: str) -> dict[str, Any]:
    return {
        "phone": phone,
        "email": email,
        "campus": "中心校区",
        "major": "软件工程",
        "introduction": "已报名赛事列表验收用户",
        "tags": [],
    }


def register_student(runner: Runner, suffix: str) -> tuple[int, str]:
    cas_id = f"20260903{suffix:04d}"
    name = f"报名列表测试学生{suffix}"
    temporary = runner.case("测试数据准备", f"学生{suffix}生成注册令牌", "POST", "/auth/test",
                            body={"casId": cas_id, "name": name})["data"]
    token = runner.case(
        "测试数据准备", f"学生{suffix}完成注册", "POST", "/student",
        body={"token": temporary, **student_payload(f"1380000{suffix:04d}",
                                                       f"registered-{suffix}@example.test")},
        check=lambda p: isinstance(p.get("data"), dict) and bool(p["data"].get("token")),
    )["data"]["token"]
    return token_user_id(token), token


def seed_registrations(container: str, database: str, current_user_id: int,
                       other_user_id: int) -> list[str]:
    suffix = uuid.uuid4().hex[:10]
    # 数据库夹具使用 ASCII，避免容器内 mysql 客户端默认字符集影响断言。
    names = [f"registered-events-{suffix}-alpha", f"registered-events-{suffix}-beta",
             f"registered-events-{suffix}-gamma", f"registered-events-{suffix}-isolated"]
    now = "2026-09-03 12:00:00"
    statements = []
    for name in names:
        statements.append(
            "INSERT INTO event (name, reg_beg, reg_end, live_beg, live_end, introduction, tags, notice, "
            "version, create_time, update_time) VALUES "
            f"('{name}', '2030-01-01 00:00:00', '2030-01-02 00:00:00', '2030-01-03 00:00:00', "
            f"'2030-01-04 00:00:00', 'registered-events fixture', 'API,acceptance', '', 1, '{now}', '{now}');"
        )
    mysql(container, database, "\n".join(statements))
    rows = mysql(container, database, "SELECT event_id, name FROM event WHERE name IN (" +
                 ",".join(f"'{name}'" for name in names) + ");")
    event_ids = {name: int(event_id) for event_id, name in
                 (line.split("\t", 1) for line in rows.strip().splitlines())}
    for index, name in enumerate(names):
        mysql(container, database,
              "INSERT INTO track (event_id, name, desc_md, version, create_time, update_time) VALUES "
              f"({event_ids[name]}, 'default-track', 'acceptance track', 1, '{now}', '{now}');")
    track_rows = mysql(container, database, "SELECT track_id, event_id FROM track WHERE event_id IN (" +
                       ",".join(str(event_ids[name]) for name in names) + ");")
    track_ids = {int(event_id): int(track_id) for track_id, event_id in
                 (line.split("\t", 1) for line in track_rows.strip().splitlines())}
    registrations = [
        (current_user_id, names[0], "2026-09-03 12:00:00"),
        (current_user_id, names[1], "2026-09-03 12:01:00"),
        (current_user_id, names[2], "2026-09-03 12:02:00"),
        (other_user_id, names[3], "2026-09-03 12:03:00"),
    ]
    for user_id, name, update_time in registrations:
        event_id = event_ids[name]
        mysql(container, database,
              "INSERT INTO registration (user_id, event_id, track_id, team_id, version, create_time, update_time) "
              f"VALUES ({user_id}, {event_id}, {track_ids[event_id]}, NULL, 1, '{update_time}', '{update_time}');")
    return names


def render_report(report: dict[str, Any], output: Path) -> None:
    def esc(value: Any) -> str:
        return html.escape(str(value), quote=True)

    grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for item in report["results"]:
        grouped[item["group"]].append(item)
    sections = "".join(
        f"<section><h2>{esc(group)} <small>{sum(x['passed'] for x in items)}/{len(items)}</small></h2>"
        "<div class='table'><table><thead><tr><th>方法</th><th>路径</th><th>场景</th><th>期望</th>"
        "<th>实际</th><th>耗时</th><th>结果</th></tr></thead><tbody>" +
        "".join(
            f"<tr><td>{esc(item['method'])}</td><td><code>{esc(item['path'])}</code></td>"
            f"<td>{esc(item['name'])}</td><td>{esc(item['expected'])}</td><td>{esc(item['actual'])}</td>"
            f"<td>{esc(item['durationMs'])} ms</td><td class={'pass' if item['passed'] else 'fail'}>"
            f"{'通过' if item['passed'] else '失败'}</td></tr>" for item in items
        ) + "</tbody></table></div></section>"
        for group, items in grouped.items()
    )
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(f"""<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\">
<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>已报名赛事列表 API 验证报告</title>
<style>body{{margin:0;background:#f5f7fb;color:#182230;font:14px/1.55 -apple-system,BlinkMacSystemFont,\"Segoe UI\",sans-serif}}main{{max-width:1120px;margin:auto;padding:34px 20px 64px}}header{{background:#153d81;color:#fff;border-radius:18px;padding:28px 32px}}h1{{margin:0 0 8px;font-size:27px}}header p{{margin:0;opacity:.86}}.stats{{display:flex;gap:14px;margin-top:20px;flex-wrap:wrap}}.stat{{background:#ffffff21;padding:12px 16px;border-radius:10px;min-width:120px}}.stat b{{display:block;font-size:23px}}section{{background:#fff;margin-top:18px;border:1px solid #e4e9f2;border-radius:14px;padding:22px;box-shadow:0 5px 20px #1720330d}}h2{{font-size:18px;margin:0 0 13px}}h2 small,.pass{{color:#087a55;font-weight:700}}.fail{{color:#bc2537;font-weight:700}}.table{{overflow:auto}}table{{border-collapse:collapse;width:100%;min-width:780px}}th,td{{padding:10px;border-bottom:1px solid #e9edf4;text-align:left;vertical-align:top}}th{{background:#fafbfe;color:#657184;font-size:12px}}code{{font:12px ui-monospace,SFMono-Regular,monospace;color:#294c9a}}.note{{color:#526070}}@media(max-width:650px){{main{{padding:16px 10px}}header{{padding:22px}}}}</style></head>
<body><main><header><h1>已报名赛事列表 API 验证报告</h1><p>外部 Python 客户端通过真实 Spring Boot HTTP 接口验证当前学生的报名筛选、分页排序、身份隔离与参数校验。</p>
<div class=\"stats\"><div class=\"stat\"><b>{report['passed']}</b>通过</div><div class=\"stat\"><b>{report['failed']}</b>失败</div><div class=\"stat\"><b>{report['total']}</b>总场景</div><div class=\"stat\"><b>{'PASS' if report['failed'] == 0 else 'FAIL'}</b>结论</div></div></header>
<section><h2>执行边界</h2><p class=\"note\">服务：<code>{esc(report['baseUrl'])}</code>；数据库：隔离 MySQL schema <code>{esc(report['database'])}</code>；执行时间：{esc(report['startedAt'])} 至 {esc(report['finishedAt'])}。学生通过 HTTP 注册；赛事与报名关系为隔离夹具。报告不包含令牌、密钥或数据库凭据。</p></section>{sections}</main></body></html>""", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://127.0.0.1:18083")
    parser.add_argument("--mysql-container", default="mysql")
    parser.add_argument("--database", required=True)
    parser.add_argument("--result", default="registered-events-api-test-results.json")
    parser.add_argument("--report", default="reports/registered-events-api-test-report.html")
    args = parser.parse_args()

    started = datetime.now(timezone.utc)
    runner = Runner(args.base_url)
    current_user_id, current_student = register_student(runner, 1)
    other_user_id, other_student = register_student(runner, 2)
    names = seed_registrations(args.mysql_container, args.database, current_user_id, other_user_id)

    runner.case("鉴权与参数", "匿名用户不能读取已报名赛事", "GET", "/registration/list?page=1&size=2",
                status=401, code=1004)
    runner.case("鉴权与参数", "页码必须为正数", "GET", "/registration/list?page=0&size=2", current_student,
                status=400, code=4000)
    runner.case("当前学生筛选与排序", "第一页只返回本人最近报名的两场赛事", "GET",
                "/registration/list?page=1&size=2", current_student,
                check=lambda p: p["data"]["total"] == 3 and p["data"]["pages"] == 2
                and [event["name"] for event in p["data"]["records"]] == [names[2], names[1]])
    runner.case("分页", "第二页返回剩余赛事", "GET", "/registration/list?page=2&size=2", current_student,
                check=lambda p: p["data"]["current"] == 2 and p["data"]["size"] == 2
                and [event["name"] for event in p["data"]["records"]] == [names[0]])
    runner.case("用户隔离", "另一学生只看到自己的报名赛事", "GET", "/registration/list?page=1&size=10", other_student,
                check=lambda p: p["data"]["total"] == 1
                and [event["name"] for event in p["data"]["records"]] == [names[3]])

    report = {
        "baseUrl": args.base_url,
        "database": args.database,
        "startedAt": started.isoformat(),
        "finishedAt": datetime.now(timezone.utc).isoformat(),
        "results": runner.results,
        "total": len(runner.results),
        "passed": sum(item["passed"] for item in runner.results),
        "failed": sum(not item["passed"] for item in runner.results),
    }
    Path(args.result).write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    render_report(report, Path(args.report))
    print(f"passed={report['passed']} failed={report['failed']} total={report['total']} report={args.report}")
    if report["failed"]:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
