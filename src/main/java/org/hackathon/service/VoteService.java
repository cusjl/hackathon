package org.hackathon.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.hackathon.data.enums.ResultCode;
import org.hackathon.data.po.Phase;
import org.hackathon.data.po.Submission;
import org.hackathon.data.po.Team;
import org.hackathon.data.po.Vote;
import org.hackathon.data.vo.VoteCastVO;
import org.hackathon.data.vo.VoteRankVO;
import org.hackathon.data.vo.VoteStatusVO;
import org.hackathon.exception.BusinessException;
import org.hackathon.mapper.SubmissionMapper;
import org.hackathon.mapper.TeamMapper;
import org.hackathon.mapper.VoteMapper;
import org.hackathon.security.Context;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoteService {

    //需求文档：每日票数上限缺省为每人每天 3 票
    private static final int DEFAULT_DAILY_CAP = 3;

    private final VoteMapper voteMapper;
    private final SubmissionMapper submissionMapper;
    private final TeamMapper teamMapper;

    /**
     * 为作品投一票。鉴权层已保证 CAS 登录且处于投票时间窗内，
     * 此处再校验本轮开关、防重复与每日限额。
     */
    public VoteCastVO cast(Context ctx) {
        Submission submission = ctx.submission();
        Phase phase = ctx.phase();
        requirePollEnabled(phase);
        int cap = dailyCap(phase);
        LocalDate today = LocalDate.now();
        long used = voteMapper.countToday(ctx.userId(), phase.getPhaseId(), today);
        if (used >= cap) {
            throw new BusinessException(ResultCode.VOTE_DAILY_LIMIT);
        }
        if (voteMapper.existsByUserAndSubmission(ctx.userId(), submission.getSubmissionId())) {
            throw new BusinessException(ResultCode.VOTE_REPEAT);
        }
        LocalDateTime now = LocalDateTime.now();
        Vote vote = new Vote(null, ctx.userId(), phase.getPhaseId(),
                submission.getSubmissionId(), today, now, now);
        try {
            voteMapper.insert(vote);
        } catch (DuplicateKeyException e) {
            //并发重复投票由唯一键兜底
            throw new BusinessException(ResultCode.VOTE_REPEAT);
        }
        return new VoteCastVO(submission.getSubmissionId(),
                voteMapper.countBySubmission(submission.getSubmissionId()),
                (int) used + 1, cap - (int) used - 1);
    }

    /**
     * 本轮最佳人气作品榜，按票数降序、同票按作品id升序。
     * 未开启大众投票的轮次返回空榜；匿名可访问，voted 标记当前用户是否已投。
     */
    public List<VoteRankVO> ranking(Context ctx) {
        Phase phase = ctx.phase();
        if (!Boolean.TRUE.equals(phase.getPoll())) return List.of();

        List<Submission> submissions = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>().eq(Submission::getPhaseId, phase.getPhaseId()));
        List<Vote> votes = voteMapper.selectByPhase(phase.getPhaseId());
        Map<Integer, Long> counts = votes.stream()
                .collect(Collectors.groupingBy(Vote::getSubmissionId, Collectors.counting()));
        Set<Integer> votedIds = ctx.isAuthenticated()
                ? votes.stream().filter(v -> v.getUserId().equals(ctx.userId()))
                        .map(Vote::getSubmissionId).collect(Collectors.toSet())
                : Set.of();

        List<Integer> teamIds = submissions.stream().map(Submission::getTeamId).distinct().toList();
        Map<Integer, Team> teams = teamIds.isEmpty() ? Map.of()
                : teamMapper.selectBatchIds(teamIds).stream()
                        .collect(Collectors.toMap(Team::getTeamId, Function.identity()));

        List<VoteRankVO> ranking = new ArrayList<>();
        for (Submission submission : submissions) {
            VoteRankVO vo = new VoteRankVO();
            vo.setSubmissionId(submission.getSubmissionId());
            vo.setTeamId(submission.getTeamId());
            Team team = teams.get(submission.getTeamId());
            vo.setTeamName(team == null ? null : team.getName());
            vo.setVoteCount(counts.getOrDefault(submission.getSubmissionId(), 0L));
            vo.setVoted(ctx.isAuthenticated() ? votedIds.contains(submission.getSubmissionId()) : null);
            ranking.add(vo);
        }
        ranking.sort(Comparator.comparing(VoteRankVO::getVoteCount).reversed()
                .thenComparing(VoteRankVO::getSubmissionId));
        for (int i = 0; i < ranking.size(); i++) {
            ranking.get(i).setRank(i + 1);
        }
        return ranking;
    }

    /**
     * 当前登录用户在本轮的投票状态
     */
    public VoteStatusVO status(Context ctx) {
        Phase phase = ctx.phase();
        boolean pollOn = Boolean.TRUE.equals(phase.getPoll());
        int cap = pollOn ? dailyCap(phase) : 0;
        int used = (int) voteMapper.countToday(ctx.userId(), phase.getPhaseId(), LocalDate.now());
        List<Integer> votedIds = voteMapper.selectByPhase(phase.getPhaseId()).stream()
                .filter(v -> v.getUserId().equals(ctx.userId()))
                .map(Vote::getSubmissionId).distinct().sorted().toList();

        VoteStatusVO vo = new VoteStatusVO();
        vo.setPhaseId(phase.getPhaseId());
        vo.setPoll(pollOn);
        vo.setDailyCap(cap);
        vo.setTodayUsed(used);
        vo.setTodayRemaining(Math.max(0, cap - used));
        vo.setVotedSubmissionIds(votedIds);
        return vo;
    }

    private void requirePollEnabled(Phase phase) {
        if (!Boolean.TRUE.equals(phase.getPoll())) {
            throw new BusinessException(ResultCode.POLL_NOT_ENABLED);
        }
    }

    private int dailyCap(Phase phase) {
        return phase.getPollDailyCap() == null ? DEFAULT_DAILY_CAP : phase.getPollDailyCap();
    }
}
