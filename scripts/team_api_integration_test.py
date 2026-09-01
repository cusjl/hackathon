#!/usr/bin/env python3
"""组队模块真实 HTTP API 验收。

测试数据由调用方放入独立数据库；本脚本只通过公开 HTTP API 操作业务状态，
不会读取 .env、数据库凭据或对象存储凭据。结果写为 JSON，供 HTML 报告生成器使用。
"""

from __future__ import annotations

import argparse
import json
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime
from pathlib import Path
from typing import Any, Callable


class Runner:
    def __init__(self, base_url: str) -> None:
        self.base = base_url.rstrip("/")
        self.results: list[dict[str, Any]] = []
        self.tokens: dict[int, str] = {}

    @staticmethod
    def display_path(path: str) -> str:
        parts = urllib.parse.urlsplit(path)
        if not parts.query:
            return path
        query = [(key, "<redacted>" if key.lower() in {"token", "code", "password"} else value)
                 for key, value in urllib.parse.parse_qsl(parts.query, keep_blank_values=True)]
        return urllib.parse.urlunsplit((parts.scheme, parts.netloc, parts.path,
                                        urllib.parse.urlencode(query), parts.fragment))

    def raw(self, method: str, path: str, token: str | None = None,
            body: Any = None) -> tuple[int, Any, float]:
        data = None if body is None else json.dumps(body, ensure_ascii=False).encode()
        headers = {"Accept": "application/json"}
        if data is not None:
            headers["Content-Type"] = "application/json"
        if token:
            headers["Authorization"] = f"Bearer {token}"
        request = urllib.request.Request(self.base + path, data=data, headers=headers, method=method)
        started = time.perf_counter()
        try:
            with urllib.request.urlopen(request, timeout=10) as response:
                status = response.status
                raw = response.read()
        except urllib.error.HTTPError as error:
            status = error.code
            raw = error.read()
        elapsed = round((time.perf_counter() - started) * 1000, 1)
        try:
            payload = json.loads(raw.decode()) if raw else None
        except json.JSONDecodeError:
            payload = {"raw": raw.decode(errors="replace")}
        return status, payload, elapsed

    def case(self, group: str, name: str, method: str, path: str,
             token: str | None = None, body: Any = None,
             status: int = 200, code: int = 200,
             check: Callable[[Any], bool] | None = None,
             expected: str | None = None) -> Any:
        actual_status, payload, elapsed = self.raw(method, path, token, body)
        actual_code = payload.get("code") if isinstance(payload, dict) else None
        passed = actual_status == status and actual_code == code
        if passed and check is not None:
            try:
                passed = bool(check(payload))
            except Exception:
                passed = False
        self.results.append({
            "group": group,
            "name": name,
            "method": method,
            "path": self.display_path(path),
            "expected": expected or f"HTTP {status} / code {code}",
            "actual": f"HTTP {actual_status} / code {actual_code}",
            "durationMs": elapsed,
            "passed": passed,
            "message": payload.get("msg") if isinstance(payload, dict) else None,
        })
        if not passed:
            raise AssertionError(f"{name}: {self.results[-1]} payload={payload}")
        return payload

    def login(self, user_id: int) -> str:
        cas_id = f"2026000000{user_id:02d}"
        temp = self.case("认证准备", f"用户{user_id}生成交换令牌", "POST", "/auth/test",
                         body={"name": f"测试用户{user_id}", "casId": cas_id})["data"]
        path = "/auth/exchange?" + urllib.parse.urlencode({"token": temp})
        token = self.case("认证准备", f"用户{user_id}兑换访问令牌", "GET", path,
                          check=lambda p: isinstance(p.get("data"), dict)
                          and bool(p["data"].get("token")))["data"]["token"]
        self.tokens[user_id] = token
        return token


def data(payload: dict[str, Any]) -> Any:
    value = payload.get("data")
    if value is None:
        raise AssertionError(f"response data missing: {payload}")
    return value


def run(base_url: str) -> Runner:
    r = Runner(base_url)
    for user_id in range(1, 8):
        r.login(user_id)
    t = r.tokens

    r.case("鉴权与隔离", "匿名用户不能读取邀请", "GET", "/team/invitations",
           status=401, code=1004)

    config = r.case("赛事人数配置", "赛管配置队伍人数为2至4人", "PUT",
                    "/event/1/team-config", t[7],
                    {"minSize": 2, "maxSize": 4, "version": 1})
    assert config["data"] is None
    r.case("赛事人数配置", "普通选手不能修改人数配置", "PUT",
           "/event/1/team-config", t[1],
           {"minSize": 1, "maxSize": 5, "version": 2}, status=403, code=1011)
    r.case("赛事人数配置", "赛事详情返回人数上下限", "GET", "/event/1", t[1],
           check=lambda p: p["data"]["teamMinSize"] == 2 and p["data"]["teamMaxSize"] == 4)

    team1 = data(r.case("基础组队", "队长创建队伍并获得邀请码", "POST", "/team/1", t[1],
                        {"name": "星火智能队", "introduction": "AI产品与交互", "type": "跨校区"},
                        check=lambda p: len(p["data"]["inviteCode"]) == 12))
    team1_id, team1_code = team1["teamId"], team1["inviteCode"]
    team2 = data(r.case("基础组队", "第二位队长创建管理测试队伍", "POST", "/team/1", t[6],
                        {"name": "管理员干预队", "introduction": "管理链路", "type": "跨校区"}))
    team2_id, team2_old_code = team2["teamId"], team2["inviteCode"]
    r.case("基础组队", "队员可查看队伍详情和邀请码", "GET", f"/team/{team1_id}", t[1],
           check=lambda p: p["data"]["size"] == 1 and p["data"]["inviteCode"] == team1_code)

    r.case("队伍名称检索", "未组队选手按队名模糊检索可加入队伍", "POST",
           "/team/events/1/list?page=1&size=20", t[3], {"name": "星火"},
           check=lambda p: any(x["teamId"] == team1_id and x["maxSize"] == 4
                               for x in p["data"]["records"])
           and all(x["teamId"] != team2_id for x in p["data"]["records"]))
    r.case("队伍名称检索", "不同赛道选手检索不到该队伍", "POST",
           "/team/events/1/list?page=1&size=20", t[5], {"name": "星火"},
           check=lambda p: not p["data"]["records"])

    r.case("一致性防线", "不同赛道报名者不能通过teamId入队", "POST",
           f"/team/{team1_id}/join", t[5], status=409, code=3118)

    recommendation_id = data(r.case("选手自荐墙", "未组队选手发布自荐名片", "PUT",
                                    "/team/events/1/recommendation", t[2],
                                    {"introduction": "擅长交互原型与提示词工程",
                                     "skills": ["UI/UX设计", "Prompt工程"]}))
    r.case("选手自荐墙", "队长按技能检索自荐名片", "POST",
           "/team/events/1/recommendations/list?page=1&size=20", t[1],
           {"trackId": 1, "skill": "Prompt工程"},
           check=lambda p: any(x["recommendationId"] == recommendation_id
                               for x in p["data"]["records"]))

    invitation_id = data(r.case("组队邀请", "队长发送待确认邀请", "POST",
                                f"/team/{team1_id}/invite", t[1], {"userId": 2}))
    r.case("组队邀请", "同一待处理邀请不能重复发送", "POST",
           f"/team/{team1_id}/invite", t[1], {"userId": 2}, status=409, code=4001)
    r.case("鉴权与隔离", "其他选手不能代替被邀请人接受邀请", "POST",
           f"/team/{team1_id}/invitations/{invitation_id}/accept", t[3],
           status=404, code=3126)
    r.case("组队邀请", "被邀请人可查看邀请", "GET", "/team/invitations", t[2],
           check=lambda p: any(x["invitationId"] == invitation_id and x["status"] == "待处理"
                               for x in p["data"]))
    r.case("组队邀请", "被邀请人拒绝邀请", "POST",
           f"/team/{team1_id}/invitations/{invitation_id}/reject", t[2])
    invitation_id = data(r.case("组队邀请", "拒绝后队长可重新发送邀请", "POST",
                                f"/team/{team1_id}/invite", t[1], {"userId": 2}))

    recruitment_id = data(r.case("队伍招募墙", "队长发布技能缺口", "POST",
                                 f"/team/{team1_id}/recruitments", t[1],
                                 {"title": "招募AI架构同学", "description": "负责Agent工作流",
                                  "requiredTags": ["AI Agent架构"], "vacancies": 3}))
    r.case("容量闭环", "招募人数不能超过剩余名额", "POST",
           f"/team/{team1_id}/recruitments", t[1],
           {"title": "超额招募", "requiredTags": [], "vacancies": 4},
           status=409, code=3123)
    r.case("队伍招募墙", "选手可检索开放招募信息", "POST",
           "/team/events/1/recruitments/list?page=1&size=20", t[3],
           {"trackId": 1, "skill": "AI Agent架构"},
           check=lambda p: any(x["recruitmentId"] == recruitment_id
                               for x in p["data"]["records"]))
    application_id = data(r.case("入队申请", "未组队选手提交入队申请", "POST",
                                 f"/team/recruitments/{recruitment_id}/applications", t[3]))
    r.case("入队申请", "队长查看申请人资料", "GET", f"/team/{team1_id}/applications", t[1],
           check=lambda p: any(x["applicationId"] == application_id and x["userId"] == 3
                               for x in p["data"]))
    r.case("入队申请", "队长接受申请并完成入队", "POST",
           f"/team/{team1_id}/applications/{application_id}/accept", t[1])
    r.case("入队申请", "已处理申请不能重复接受", "POST",
           f"/team/{team1_id}/applications/{application_id}/accept", t[1],
           status=409, code=3125)
    r.case("通知闭环", "申请人收到审批结果通知", "GET",
           "/notification?page=1&size=20", t[3],
           check=lambda p: any(x["type"] == "入队申请结果" for x in p["data"]["records"]))

    r.case("组队邀请", "被邀请人接受邀请并入队", "POST",
           f"/team/{team1_id}/invitations/{invitation_id}/accept", t[2])
    r.case("选手自荐墙", "入队后自荐名片自动撤下", "POST",
           "/team/events/1/recommendations/list?page=1&size=20", t[1], {},
           check=lambda p: all(x["userId"] != 2 for x in p["data"]["records"]))

    r.case("私密邀请码", "选手使用邀请码加入队伍", "POST", "/team/join-code", t[4],
           {"code": team1_code})
    r.case("容量闭环", "队伍达到赛事上限4人", "GET", f"/team/{team1_id}", t[1],
           check=lambda p: p["data"]["size"] == 4)
    r.case("队伍名称检索", "满员队伍不再出现在可加入队伍检索中", "POST",
           "/team/events/1/list?page=1&size=20", t[7], {"name": "星火"},
           check=lambda p: not p["data"]["records"])
    r.case("队长转让", "当前队长把职责转让给本队成员", "POST",
           f"/team/{team1_id}/leader/transfer", t[1], {"userId": 2})
    r.case("队长转让", "队伍详情只标记一名新队长", "GET", f"/team/{team1_id}", t[2],
           check=lambda p: sum(1 for x in p["data"]["members"] if x["leader"]) == 1
           and any(x["userId"] == 2 and x["leader"] for x in p["data"]["members"]))
    r.case("队长转让", "原队长转让后不再具有队长权限", "POST",
           f"/team/{team1_id}/leader/transfer", t[1], {"userId": 3},
           status=403, code=1013)
    r.case("队长转让", "不能把队长转让给非本队成员", "POST",
           f"/team/{team1_id}/leader/transfer", t[2], {"userId": 5},
           status=409, code=3117)
    r.case("通知闭环", "新队长收到队长变更通知", "GET",
           "/notification?page=1&size=20", t[2],
           check=lambda p: any(x["type"] == "队长变更" for x in p["data"]["records"]))
    r.case("队长转让", "新队长可以把职责转回原队长", "POST",
           f"/team/{team1_id}/leader/transfer", t[2], {"userId": 1})
    r.case("容量闭环", "满员后开放招募自动关闭", "POST",
           "/team/events/1/recruitments/list?page=1&size=20", t[1], {},
           check=lambda p: all(x["teamId"] != team1_id for x in p["data"]["records"]))
    r.case("容量闭环", "满员队伍不能继续邀请", "POST", f"/team/{team1_id}/invite", t[1],
           {"userId": 7}, status=409, code=3115)

    new_code = data(r.case("私密邀请码", "队长刷新第二支队伍邀请码", "POST",
                           f"/team/{team2_id}/invite-code", t[6],
                           check=lambda p: len(p["data"]) == 12 and p["data"] != team2_old_code))
    r.case("私密邀请码", "旧邀请码立即失效", "POST", "/team/join-code", t[5],
           {"code": team2_old_code}, status=404, code=3119)
    r.case("一致性防线", "新邀请码仍执行同赛道校验", "POST", "/team/join-code", t[5],
           {"code": new_code}, status=409, code=3118)

    recommendation7 = data(r.case("选手自荐墙", "另一选手发布自荐名片", "PUT",
                                  "/team/events/1/recommendation", t[7],
                                  {"introduction": "后端与测试", "skills": ["AI工具集成"]}))
    r.case("选手自荐墙", "选手可以主动撤下自荐名片", "DELETE",
           "/team/events/1/recommendation", t[7])
    r.case("选手自荐墙", "已撤下名片不再出现在检索结果", "POST",
           "/team/events/1/recommendations/list?page=1&size=20", t[6], {},
           check=lambda p: all(x["recommendationId"] != recommendation7
                               for x in p["data"]["records"]))

    recruitment2 = data(r.case("入队申请", "第二支队伍发布审批测试缺口", "POST",
                               f"/team/{team2_id}/recruitments", t[6],
                               {"title": "招募测试同学", "description": "验证拒绝流程",
                                "requiredTags": ["测试"], "vacancies": 2}))
    application7 = data(r.case("入队申请", "选手向第二支队伍申请入队", "POST",
                               f"/team/recruitments/{recruitment2}/applications", t[7]))
    r.case("入队申请", "队长拒绝入队申请", "POST",
           f"/team/{team2_id}/applications/{application7}/reject", t[6])
    r.case("通知闭环", "被拒绝选手收到审批结果通知", "GET",
           "/notification?page=1&size=20", t[7],
           check=lambda p: any(x["type"] == "入队申请结果" for x in p["data"]["records"]))

    r.case("管理员干预", "赛管分页检索赛事队伍", "POST",
           "/team/events/1/admin/list?page=1&size=20", t[7], {"name": "管理员"},
           check=lambda p: any(x["teamId"] == team2_id for x in p["data"]["records"]))
    r.case("管理员干预", "普通选手不能使用后台队伍检索", "POST",
           "/team/events/1/admin/list?page=1&size=20", t[1], {}, status=403, code=1011)
    r.case("管理员干预", "赛管人工添加队员", "POST", f"/team/{team2_id}/members", t[7],
           {"userId": 7})
    r.case("管理员设置队长", "赛管不能把非本队成员设置为队长", "PUT",
           f"/team/{team2_id}/leader", t[7], {"userId": 5}, status=409, code=3117)
    r.case("管理员设置队长", "赛管把本队成员设置为队长", "PUT",
           f"/team/{team2_id}/leader", t[7], {"userId": 7})
    r.case("管理员设置队长", "管理员设置后队伍仍只有一名队长", "GET",
           f"/team/{team2_id}", t[7],
           check=lambda p: sum(1 for x in p["data"]["members"] if x["leader"]) == 1
           and any(x["userId"] == 7 and x["leader"] for x in p["data"]["members"]))
    r.case("管理员设置队长", "赛管将队长设置回原队长", "PUT",
           f"/team/{team2_id}/leader", t[7], {"userId": 6})
    r.case("管理员干预", "赛管人工移出队员", "DELETE", f"/team/{team2_id}/kick", t[7],
           {"userId": 7})
    team2_info = data(r.case("管理员干预", "赛管读取干预后的队伍", "GET",
                             f"/team/{team2_id}", t[7]))
    r.case("管理员干预", "赛管修改队伍名称", "PUT", f"/team/{team2_id}", t[7],
           {"name": "管理员已修正队", "introduction": "违规信息已修正",
            "type": "跨校区", "version": team2_info["version"]})
    r.case("管理员干预", "赛管解散违规队伍", "DELETE", f"/team/{team2_id}", t[7])
    r.case("管理员干预", "解散后队伍不可再查询", "GET", f"/team/{team2_id}", t[7],
           status=404, code=3113)

    return r


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://127.0.0.1:18089")
    parser.add_argument("--output", default="team-api-test-results.json")
    args = parser.parse_args()
    started = datetime.now().astimezone()
    runner = run(args.base_url)
    finished = datetime.now().astimezone()
    passed = sum(1 for item in runner.results if item["passed"])
    result = {
        "title": "组队与人才招募 API 真实环境验证",
        "baseUrl": args.base_url,
        "database": "独立 MySQL 8.4 数据库（名称记录于执行环境，不含凭据）",
        "migration": "Flyway V1–V9 从空库完整执行",
        "startedAt": started.isoformat(timespec="seconds"),
        "finishedAt": finished.isoformat(timespec="seconds"),
        "total": len(runner.results),
        "passed": passed,
        "failed": len(runner.results) - passed,
        "results": runner.results,
    }
    Path(args.output).write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
    print(f"{passed}/{len(runner.results)} scenarios passed; result={args.output}")


if __name__ == "__main__":
    main()
