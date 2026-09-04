package org.hackathon.service;

import org.hackathon.data.enums.ReviewStatus;
import org.hackathon.data.po.ReviewAssignment;
import org.hackathon.data.po.Submission;
import org.hackathon.data.po.Team;
import org.hackathon.data.vo.SubmissionResultVO;
import org.hackathon.mapper.FileObjectMapper;
import org.hackathon.mapper.RegistrationMapper;
import org.hackathon.mapper.ReviewAssignmentMapper;
import org.hackathon.mapper.ReviewFlagMapper;
import org.hackathon.mapper.ReviewScoreMapper;
import org.hackathon.mapper.StudentMapper;
import org.hackathon.mapper.SubmissionMapper;
import org.hackathon.mapper.TeamMapper;
import org.hackathon.mapper.UserMapper;
import org.hackathon.security.Context;
import org.hackathon.security.Role;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReviewServiceTest {

    @Test
    void submissionOwnerSeesAggregateButNotJudgeDetails() {
        ReviewAssignmentMapper assignmentMapper = mock(ReviewAssignmentMapper.class);
        ReviewAssignService assignService = mock(ReviewAssignService.class);
        ReviewService service = new ReviewService(
                assignmentMapper, mock(ReviewScoreMapper.class), mock(ReviewFlagMapper.class),
                mock(SubmissionMapper.class), mock(TeamMapper.class), mock(UserMapper.class),
                mock(StudentMapper.class), mock(RegistrationMapper.class), mock(FileObjectMapper.class),
                mock(ReviewDimensionService.class), assignService, mock(ReviewFlagService.class),
                mock(SubmissionService.class), mock(FileService.class));
        Context ctx = mock(Context.class);
        Submission submission = new Submission();
        submission.setSubmissionId(7);
        submission.setTeamId(3);
        Team team = new Team();
        team.setTeamId(3);
        team.setName("星火队");
        ReviewAssignment assignment = new ReviewAssignment();
        assignment.setAssignmentId(11);
        assignment.setSubmissionId(7);
        assignment.setJudgeId(9);
        assignment.setStatus(ReviewStatus.DONE);
        assignment.setTotalScore(new BigDecimal("91.50"));
        assignment.setSubmitTime(LocalDateTime.now());

        when(ctx.submission()).thenReturn(submission);
        when(ctx.team()).thenReturn(team);
        when(ctx.is(Role.EVENT_ADMIN)).thenReturn(false);
        when(assignmentMapper.selectBySubmission(7)).thenReturn(List.of(assignment));
        when(assignService.nameOf(List.of(9))).thenReturn(Map.of(9, "评委甲"));

        SubmissionResultVO result = service.getResult(ctx);

        assertEquals(new BigDecimal("91.50"), result.getFinalScore());
        assertEquals(1, result.getScoredCount());
        assertEquals(List.of(), result.getJudgeScores());
    }

    @Test
    void eventAdminRetainsJudgeDetails() {
        ReviewAssignmentMapper assignmentMapper = mock(ReviewAssignmentMapper.class);
        ReviewAssignService assignService = mock(ReviewAssignService.class);
        ReviewService service = new ReviewService(
                assignmentMapper, mock(ReviewScoreMapper.class), mock(ReviewFlagMapper.class),
                mock(SubmissionMapper.class), mock(TeamMapper.class), mock(UserMapper.class),
                mock(StudentMapper.class), mock(RegistrationMapper.class), mock(FileObjectMapper.class),
                mock(ReviewDimensionService.class), assignService, mock(ReviewFlagService.class),
                mock(SubmissionService.class), mock(FileService.class));
        Context ctx = mock(Context.class);
        Submission submission = new Submission();
        submission.setSubmissionId(7);
        submission.setTeamId(3);
        Team team = new Team();
        team.setTeamId(3);
        team.setName("星火队");
        ReviewAssignment assignment = new ReviewAssignment();
        assignment.setAssignmentId(11);
        assignment.setSubmissionId(7);
        assignment.setJudgeId(9);
        assignment.setStatus(ReviewStatus.DONE);
        assignment.setTotalScore(new BigDecimal("91.50"));
        assignment.setSubmitTime(LocalDateTime.now());

        when(ctx.submission()).thenReturn(submission);
        when(ctx.team()).thenReturn(team);
        when(ctx.is(Role.EVENT_ADMIN)).thenReturn(true);
        when(assignmentMapper.selectBySubmission(7)).thenReturn(List.of(assignment));
        when(assignService.nameOf(List.of(9))).thenReturn(Map.of(9, "评委甲"));

        SubmissionResultVO result = service.getResult(ctx);

        assertEquals(1, result.getJudgeScores().size());
        assertEquals("评委甲", result.getJudgeScores().getFirst().getJudgeName());
    }
}
