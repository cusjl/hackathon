# 数据面板接口与统计口径

数据面板按单场赛事统计，独立于风采墙。Public 展示汇总数据，Admin 提供管理概况及对应明细。

## 配置可公开的统计项

使用 `GET/PUT /dashboard/event/{eventId}/config`，仅本赛事管理员或超管可以操作。没有整体公开布尔开关。

```json
{
  "publicMetrics": ["PARTICIPANT_COUNT", "CAMPUS_DISTRIBUTION", "TEAM_COUNT"],
  "version": 0
}
```

未配置时返回 `publicMetrics=[]`、`version=0`。首次保存使用 0；以后携带查询返回的当前版本。列表全量替换，`[]` 取消全部公开；缺省、null、重复值、未知统计项均拒绝。并发写入只有一个请求能成功，其他请求返回 409 / 4002。

| 配置值 | metrics 中的字段 | 口径 |
|---|---|---|
| PARTICIPANT_COUNT | participantCount | 当前本赛事报名记录数；报名唯一约束按用户与赛事去重，包含未组队者 |
| CAMPUS_DISTRIBUTION | campusDistribution | 同一报名人群按当前学生校区分组；固定八校区及未知分类，缺失/异常值不丢弃 |
| TEAM_COUNT | teamCount | 当前本赛事队伍数，包含已淘汰队伍 |
| TRACK_SUBMISSIONS | trackSubmissions | 各赛道至少提交过一次的队伍数；跨轮次和版本去重，无提交赛道为0 |
| AI_TOOLS | aiTools | 代表作品实际填报的 AI 工具，按使用项目数统计 |
| TECH_STACKS | techStacks | 代表作品实际填报的设计工具和技术栈，按使用项目数统计 |

公开接口：`GET /dashboard/event/{eventId}`。其 `metrics` 只包含被选中的字段，后端也只查询被选项。没有选中项时返回 `{}`，前端显示“暂无公开统计”。已公开但没有数据的指标保留零值/空数组，不与未公开混淆。

例如只选择 TEAM_COUNT 时：

```json
{
  "eventId": 1,
  "eventName": "示例赛事",
  "generatedAt": "2026-09-11T18:00:00",
  "publicMetrics": ["TEAM_COUNT"],
  "metrics": {"teamCount": 12}
}
```

实际响应仍包装在统一 `{code,data,msg}` 中。Public 永远不返回管理员配置版本、成员名单、评委进度或异常详情。校区分布可以推算总人数，因此按项控制是展示配置，不代表相关数值完全不可推算。

所有比例在 0～1 之间，保留六位小数，前端转换百分比。分母为0时为null。校区来自当前资料，修改学生校区后历史赛事分布会变化；当前版本未提供闭幕式永久统计快照。

## 工具与技术栈填报

`SubmitWorkDTO`、作品详情和 `SubmissionSnapshot` 新增 `aiTools`、`techStacks`。它们是可选项目统计资料，不由动态必填项开关控制。

- 每类最多30项，每项1～50字符，禁止空白或控制字符。
- 统一首尾及连续空白、大小写、常见别名；例如 cursor/Cursor、vue.js/Vue 合并。
- 常见标签保留规范显示名称，其他自定义标签统一为小写，以保证跨项目一致统计。
- 首次缺省或null表示未填报；更新缺省或null保留已有值，显式 `[]` 表示明确没有或清空。
- 提交与补交均生成包含标签的版本快照；旧快照中不存在该字段时按null处理。
- 标签存储在 submission 的两个可空 JSON 列中，避免再维护一份重复明细表；聚合使用 MySQL JSON_TABLE。

每支队伍取 `submitBeg` 最新的已提交轮次，再使用该作品的当前版本；轮次时间并列时以 `phaseId` 决定。不会合并所有历史轮次，也不会从个人技能或声明文本推断标签。

词云返回 `totalProjects`、`reportedProjects`、`coverage`、`words`、`truncated`。null不计入已填报，`[]`计入已填报但不产生词。最多展示50项，按项目数降序、名称升序；超过50项时明确标注截断。每个工具在单项目中只计一次。

## 管理员面板

`GET /dashboard/event/{eventId}/admin` 仅本赛事管理员或超管可访问，不受 publicMetrics 配置影响。

- `registration`：报名、已组队、未组队人数，队伍数，人数不足队伍数。队伍人数按实际报名成员统计，最低人数未设置时按1人。
- `phases`：各轮次提交情况、未分配作品数、待评/已完成任务数、评审完成率。
- `judges`：各轮次各评委的任务量、完成量、待评量及逾期量。只在当前仍持有的 PENDING/DONE 任务中计算完成率。
- `workItems`：尚未关联接手任务的回避记录、当前有效补交窗口、到期未补交记录。
- `metrics`：全部六项汇总统计；`publicConfig`：当前公开项配置。

### 轮次提交分母的边界

首轮使用当前赛道队伍数作为 `expectedTeams`；赛事开始后现有业务已禁止新建和解散队伍。当前系统没有完整历史晋级名册，非首轮不能可靠还原应提交名单，因此：

- `expectedTeams`、`missingTeams`、`submissionRate` 返回null。
- `denominatorAvailable=false`，`denominatorReason=HISTORICAL_ELIGIBILITY_UNAVAILABLE`。
- `knownExpectedTeams` 仅为当前轮次指针指向该轮或已在该轮提交的队伍并集。
- `knownMissingTeams` 是上述已知范围内尚未提交的数量，仅为已知下界，不能作为全轮次未提交总数。

前端对非首轮应显示“应提交名单尚不完整”，可以展示已提交数与已知未提交数，不显示推算的完整提交率。后续实现晋级历史名单后，可用真实名单替换此边界。

`unassignedWorks` 表示没有任何 PENDING/DONE 任务的作品数，不代表“未达到目标评委数量”；当前没有每轮统一目标评委数配置。

回避后自动分配和新人工补派都保留来源任务关联；历史数据中已人工补派但未建立来源关联的回避记录仍需管理员核实，不自动猜测和回填。`expiredFlags` 包括超时OPEN和系统自动关闭的记录，不包含手动关闭或已补交的记录。

### 从统计进入明细

`GET /dashboard/event/{eventId}/admin/details?kind=...&phaseId=...&page=1&size=10`

| kind | 明细 | phaseId |
|---|---|---|
| UNTEAMED | 未组队报名人员 | 不接受 |
| UNDERFILLED | 人数不足队伍 | 不接受 |
| SUBMITTED | 已提交队伍与作品 | 必填 |
| MISSING_SUBMISSION | 已知名单内未提交队伍 | 必填 |
| UNASSIGNED | 尚无在手评审任务的作品 | 必填 |
| PENDING_REVIEW | 待评任务 | 可选 |
| UNREPLACED_RECUSED | 未关联接手任务的回避记录 | 可选 |
| OPEN_FLAGS | 有效补交窗口 | 可选 |
| EXPIRED_FLAGS | 超时未补交记录 | 可选 |

返回分页数据及相关实体ID，前端可进入现有队伍、作品或评审页面。phaseId必须属于路径赛事。所有查询在数据库端聚合或分页，面板使用一致的只读事务。前端可约30秒刷新一次，失败时保留旧数据并标注更新时间；不需要WebSocket。

## 迁移和验证

`V11__dashboard.sql` 增加标签列、公开项配置表和必要索引。原始作品默认未填报，原始赛事默认没有公开项；V1～V9不改动。

可复用验证脚本：`scripts/showcase_dashboard_api_integration_test.py`。必须指定独立 `hackathon_*_it_*` 数据库并启动对应服务，脚本不会清空数据库。每次运行新增带唯一后缀的数据，避免覆盖其他测试。

```bash
python3 scripts/showcase_dashboard_api_integration_test.py \
  --base-url http://127.0.0.1:18084 \
  --database hackathon_showcase_dashboard_it_20260911_final
```

服务应使用同一隔离schema，`spring.flyway.baseline-on-migrate=false`；本地MySQL会话与应用统一为Asia/Shanghai（连接初始化 `SET time_zone='+08:00'`），UTF-8使用utf8mb4。展示文件需连接独立测试S3存储；本次验证使用本机MinIO，无生产存储写入。

完整字段、类型、鉴权与响应结构见 `openapi.yaml`。
