package org.hackathon.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hackathon.data.dto.AssignJudgeDTO;
import org.hackathon.data.dto.AutoAssignDTO;
import org.hackathon.data.dto.CloseFlagDTO;
import org.hackathon.data.dto.CreateDimensionDTO;
import org.hackathon.data.dto.FlagWorkDTO;
import org.hackathon.data.dto.RecuseDTO;
import org.hackathon.data.dto.ScoreWorkDTO;
import org.hackathon.data.dto.TransferAssignmentDTO;
import org.hackathon.data.dto.UpdateDimensionDTO;
import org.hackathon.data.dto.UrgeJudgeDTO;
import org.hackathon.data.vo.*;
import org.hackathon.security.Context;
import org.hackathon.security.Require;
import org.hackathon.service.ReviewAssignService;
import org.hackathon.service.ReviewDimensionService;
import org.hackathon.service.ReviewFlagService;
import org.hackathon.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

import static org.hackathon.security.Role.*;

@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewDimensionService dimensionService;
    private final ReviewAssignService assignService;
    private final ReviewService reviewService;
    private final ReviewFlagService flagService;

    // ==================== 评分维度配置 ====================

    /**
     * 本轮评分维度列表，评委据此渲染打分表
     * @return 维度列表
     */
    @GetMapping("/phase/{phaseId}/dimension")
    @Require
    public ResponseEntity<Result<List<DimensionVO>>> listDimensions(Context ctx) {
        return Result.success(dimensionService.listDimensions(ctx), "获取成功");
    }

    /**
     * 新增评分维度
     * @param dto 维度名称、满分与权重
     * @return 新维度id
     */
    @PostMapping("/phase/{phaseId}/dimension")
    @Require(EVENT_ADMIN)
    public ResponseEntity<Result<DimensionIdVO>> createDimension(
            @RequestBody @Valid CreateDimensionDTO dto, Context ctx) {
        return Result.success(dimensionService.createDimension(dto, ctx), "创建成功");
    }

    /**
     * 修改评分维度，本轮已有评委完成打分后锁定
     * @param dimensionId 维度id
     * @param dto 维度内容+version
     * @return ok
     */
    @PutMapping("/phase/{phaseId}/dimension/{dimensionId}")
    @Require(EVENT_ADMIN)
    public ResponseEntity<Result<Void>> updateDimension(
            @PathVariable Integer dimensionId, @RequestBody @Valid UpdateDimensionDTO dto, Context ctx) {
        dimensionService.updateDimension(dimensionId, dto, ctx);
        return Result.ok();
    }

    /**
     * 删除评分维度，本轮已有评委完成打分后锁定
     * @param dimensionId 维度id
     * @return ok
     */
    @DeleteMapping("/phase/{phaseId}/dimension/{dimensionId}")
    @Require(EVENT_ADMIN)
    public ResponseEntity<Result<Void>> deleteDimension(
            @PathVariable Integer dimensionId, Context ctx) {
        dimensionService.deleteDimension(dimensionId, ctx);
        return Result.ok();
    }

    // ==================== 评委指派 ====================

    /**
     * 把若干评委指派到若干作品
     * @param dto 评委id与作品id列表
     * @return 新建与跳过的数量
     */
    @PostMapping("/phase/{phaseId}/assignment")
    @Require(EVENT_ADMIN)
    public ResponseEntity<Result<AssignResultVO>> assign(
            @RequestBody @Valid AssignJudgeDTO dto, Context ctx) {
        return Result.success(assignService.assign(dto, ctx), "指派成功");
    }

    /**
     * 按负载最轻优先为本轮每份作品自动补齐评委
     * @param dto 每份作品期望的评委数
     * @return 新建与跳过的数量
     */
    @PostMapping("/phase/{phaseId}/assignment/auto")
    @Require(EVENT_ADMIN)
    public ResponseEntity<Result<AssignResultVO>> autoAssign(
            @RequestBody @Valid AutoAssignDTO dto, Context ctx) {
        return Result.success(assignService.autoAssign(dto, ctx), "指派成功");
    }

    /**
     * 把逾期评委的待评任务移交给其他评委
     * @param assignmentId 待移交的任务id
     * @param dto 接手评委，不传则由系统挑选
     * @return 移交后的新任务
     */
    @PostMapping("/phase/{phaseId}/assignment/{assignmentId}/transfer")
    @Require(EVENT_ADMIN)
    public ResponseEntity<Result<RecuseResultVO>> transfer(
            @PathVariable Integer assignmentId,
            @RequestBody @Valid TransferAssignmentDTO dto, Context ctx) {
        return Result.success(assignService.transfer(assignmentId, dto, ctx), "移交成功");
    }

    // ==================== 评委工作台 ====================

    /**
     * 评委在本轮的待评列表
     * @return 待评任务列表
     */
    @GetMapping("/phase/{phaseId}/task")
    @Require(EVENT_JUDGE)
    public ResponseEntity<Result<List<ReviewTaskVO>>> listTasks(Context ctx) {
        return Result.success(reviewService.listTasks(ctx), "获取成功");
    }

    /**
     * 作品评审视图：作品内容 + 评分维度 + 本评委已有打分 + 异常标记
     * @return 评审视图
     */
    @GetMapping("/submission/{submissionId}")
    @Require({ASSIGNED_JUDGE, EVENT_ADMIN})
    public ResponseEntity<Result<ReviewWorkVO>> getWork(Context ctx) {
        return Result.success(reviewService.getWork(ctx), "获取成功");
    }

    /**
     * 作品在线预览：文档与录屏返回带时效的预签名直读地址，Demo 网址供 iframe 嵌入
     * @return 预览入口
     */
    @GetMapping("/submission/{submissionId}/preview")
    @Require({ASSIGNED_JUDGE, EVENT_ADMIN, SUBMISSION_OWNER})
    public ResponseEntity<Result<ReviewPreviewVO>> preview(Context ctx) {
        return Result.success(reviewService.preview(ctx), "获取成功");
    }

    /**
     * 评委逐维度打分并填写评语，评审时间窗内可重复提交覆盖
     * @param dto 各维度得分与总评语
     * @return 本次加权总分
     */
    @PostMapping("/submission/{submissionId}/score")
    @Require(value = ASSIGNED_JUDGE, window = Require.Window.REVIEW)
    public ResponseEntity<Result<BigDecimal>> score(
            @RequestBody @Valid ScoreWorkDTO dto, Context ctx) {
        return Result.success(reviewService.score(dto, ctx), "打分成功");
    }

    /**
     * 评委申请利益回避，作品随即移出其待评列表并重新分发
     * @param dto 回避理由
     * @return 回避的任务与接手评委
     */
    @PostMapping("/submission/{submissionId}/recuse")
    @Require(ASSIGNED_JUDGE)
    public ResponseEntity<Result<RecuseResultVO>> recuse(
            @RequestBody @Valid RecuseDTO dto, Context ctx) {
        return Result.success(assignService.recuse(dto, ctx), "回避成功");
    }

    // ==================== Demo 异常标记 ====================

    /**
     * 评委标记 Demo 链接或文件异常，系统通知队长并开出补交窗口
     * @param dto 异常对象、异常信息与窗口时长
     * @return 标记id与窗口截止时间
     */
    @PostMapping("/submission/{submissionId}/flag")
    @Require(value = ASSIGNED_JUDGE, window = Require.Window.REVIEW)
    public ResponseEntity<Result<FlagIdVO>> flag(
            @RequestBody @Valid FlagWorkDTO dto, Context ctx) {
        return Result.success(flagService.flag(dto, ctx), "已标记异常并开放补交窗口");
    }

    /**
     * 作品的异常标记列表
     * @return 标记列表
     */
    @GetMapping("/submission/{submissionId}/flag")
    @Require({ASSIGNED_JUDGE, EVENT_ADMIN, SUBMISSION_OWNER})
    public ResponseEntity<Result<List<ReviewFlagVO>>> listFlags(Context ctx) {
        return Result.success(flagService.listFlags(ctx.submission().getSubmissionId(),
                ctx.view(ctx.phase()) == Context.View.BLIND), "获取成功");
    }

    /**
     * 管理员提前关闭补交窗口
     * @param flagId 标记id
     * @param dto 关闭理由
     * @return ok
     */
    @PostMapping("/submission/{submissionId}/flag/{flagId}/close")
    @Require(EVENT_ADMIN)
    public ResponseEntity<Result<Void>> closeFlag(
            @PathVariable Integer flagId, @RequestBody @Valid CloseFlagDTO dto, Context ctx) {
        flagService.close(flagId, dto, ctx);
        return Result.ok();
    }

    // ==================== 打分监管与计分 ====================

    /**
     * 本轮各评委的打分进度，形如「评委A 已完成 8/10」
     * @return 进度看板
     */
    @GetMapping("/phase/{phaseId}/progress")
    @Require(EVENT_ADMIN)
    public ResponseEntity<Result<PhaseProgressVO>> getProgress(Context ctx) {
        return Result.success(assignService.getProgress(ctx), "获取成功");
    }

    /**
     * 一键催办，评委列表为空时催办本轮全部仍有待评任务的评委
     * @param dto 评委列表与催办附言
     * @return 触达的评委数与待评任务数
     */
    @PostMapping("/phase/{phaseId}/urge")
    @Require(EVENT_ADMIN)
    public ResponseEntity<Result<UrgeResultVO>> urge(
            @RequestBody @Valid UrgeJudgeDTO dto, Context ctx) {
        return Result.success(assignService.urge(dto, ctx), "催办成功");
    }

    /**
     * 单份作品的最终得分与计分明细（去极值平均）。
     * 作品所有者只能查看本队作品的汇总成绩，评委身份和逐项得分仅赛事管理员可见。
     * @return 计分结果
     */
    @GetMapping("/submission/{submissionId}/result")
    @Require({EVENT_ADMIN, SUBMISSION_OWNER})
    public ResponseEntity<Result<SubmissionResultVO>> getResult(Context ctx) {
        return Result.success(reviewService.getResult(ctx), "获取成功");
    }

    /**
     * 本轮全部作品的最终得分，按得分降序
     * @return 计分结果列表
     */
    @GetMapping("/phase/{phaseId}/result")
    @Require(EVENT_ADMIN)
    public ResponseEntity<Result<List<SubmissionResultVO>>> listResults(Context ctx) {
        return Result.success(reviewService.listResults(ctx), "获取成功");
    }
}
