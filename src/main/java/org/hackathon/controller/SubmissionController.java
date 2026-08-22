package org.hackathon.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hackathon.data.dto.SubmitWorkDTO;
import org.hackathon.data.vo.Result;
import org.hackathon.data.vo.SubmissionIdVO;
import org.hackathon.data.vo.SubmissionInfoVO;
import org.hackathon.data.vo.SubmissionVersionBriefVO;
import org.hackathon.data.vo.SubmissionVersionVO;
import org.hackathon.security.Context;
import org.hackathon.security.Require;
import org.hackathon.service.SubmissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.hackathon.security.Role.*;

@RestController
@RequestMapping("/submission")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    /**
     * 获取本队本轮作品，同时返回本轮勾选的提交项供前端渲染动态表单
     * @return 作品详情（尚未提交时仅含表单配置）
     */
    @GetMapping("/phase/{phaseId}/team/{teamId}")
    @Require({TEAM_MEMBER, EVENT_ADMIN, EVENT_JUDGE})
    public ResponseEntity<Result<SubmissionInfoVO>> getSubmission(Context ctx) {
        return Result.success(submissionService.getSubmission(ctx), "获取成功");
    }

    /**
     * 队长提交/重新提交作品，截止前可多次提交，每次生成一个版本
     * @param dto 作品内容，必填项由本轮提交设置决定
     * @return 作品id及本次版本号
     */
    @PostMapping("/phase/{phaseId}/team/{teamId}")
    @Require(value = TEAM_LEADER, window = Require.Window.SUBMIT)
    public ResponseEntity<Result<SubmissionIdVO>> submit(
            @RequestBody @Valid SubmitWorkDTO dto, Context ctx) {
        return Result.success(submissionService.submit(dto, ctx), "提交成功");
    }

    /**
     * 补交：评委打出 Demo/文件异常标记后开出的受限豁免通道。
     * 常规提交时间窗与作品锁定状态不受影响，本接口只在存在未过期的异常标记时放行，
     * 补交同样生成新版本并把版本号回写到异常标记上。
     * @param dto 作品内容，必填项仍由本轮提交设置决定
     * @return 作品id及补交产生的版本号
     */
    @PostMapping("/{submissionId}/supplement")
    @Require(value = TEAM_LEADER, window = Require.Window.SUPPLEMENT)
    public ResponseEntity<Result<SubmissionIdVO>> supplement(
            @RequestBody @Valid SubmitWorkDTO dto, Context ctx) {
        return Result.success(submissionService.supplement(dto, ctx), "补交成功");
    }

    /**
     * 按作品id查询作品详情
     * @return 作品详情
     */
    @GetMapping("/{submissionId}")
    @Require({SUBMISSION_OWNER, EVENT_ADMIN, EVENT_JUDGE})
    public ResponseEntity<Result<SubmissionInfoVO>> getSubmissionById(Context ctx) {
        return Result.success(submissionService.getSubmissionById(ctx), "获取成功");
    }

    /**
     * 版本日志列表
     * @return v1、v2 … 升序排列的版本列表
     */
    @GetMapping("/{submissionId}/version")
    @Require({SUBMISSION_OWNER, EVENT_ADMIN, EVENT_JUDGE})
    public ResponseEntity<Result<List<SubmissionVersionBriefVO>>> listVersions(Context ctx) {
        return Result.success(submissionService.listVersions(ctx), "获取成功");
    }

    /**
     * 查看某个历史版本的完整快照
     * @param versionNo 版本序号
     * @return 该版本快照
     */
    @GetMapping("/{submissionId}/version/{versionNo}")
    @Require({SUBMISSION_OWNER, EVENT_ADMIN, EVENT_JUDGE})
    public ResponseEntity<Result<SubmissionVersionVO>> getVersion(
            @PathVariable Integer versionNo, Context ctx) {
        return Result.success(submissionService.getVersion(versionNo, ctx), "获取成功");
    }
}
