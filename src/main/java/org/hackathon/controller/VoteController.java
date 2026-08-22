package org.hackathon.controller;

import lombok.RequiredArgsConstructor;
import org.hackathon.data.vo.Result;
import org.hackathon.data.vo.VoteCastVO;
import org.hackathon.data.vo.VoteRankVO;
import org.hackathon.data.vo.VoteStatusVO;
import org.hackathon.security.Context;
import org.hackathon.security.Require;
import org.hackathon.service.VoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.hackathon.security.Role.LOGGED_IN;

@RestController
@RequestMapping("/vote")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    /**
     * 为作品投一票。须 CAS 登录且处于本轮投票时间窗内；
     * 同一作品每人仅一票，每日票数受本轮 pollDailyCap（缺省 3）限制
     * @return 作品总票数与本人今日剩余票数
     */
    @PostMapping("/submission/{submissionId}")
    @Require(value = LOGGED_IN, window = Require.Window.VOTE)
    public ResponseEntity<Result<VoteCastVO>> cast(Context ctx) {
        return Result.success(voteService.cast(ctx), "投票成功");
    }

    /**
     * 本轮最佳人气作品榜，按票数降序；未开启大众投票的轮次返回空榜。
     * 匿名可访问，voted 字段标记当前登录用户是否已投
     * @return 人气榜
     */
    @GetMapping("/phase/{phaseId}/ranking")
    public ResponseEntity<Result<List<VoteRankVO>>> ranking(Context ctx) {
        return Result.success(voteService.ranking(ctx), "获取成功");
    }

    /**
     * 当前登录用户在本轮的投票状态：每日上限、今日已投与剩余票数、已投作品列表
     * @return 投票状态
     */
    @GetMapping("/phase/{phaseId}/status")
    @Require(LOGGED_IN)
    public ResponseEntity<Result<VoteStatusVO>> status(Context ctx) {
        return Result.success(voteService.status(ctx), "获取成功");
    }
}
