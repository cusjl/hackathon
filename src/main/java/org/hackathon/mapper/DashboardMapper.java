package org.hackathon.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.apache.ibatis.annotations.*;
import org.hackathon.data.enums.DashboardDetailKind;

import java.time.LocalDateTime;
import java.util.*;

@Mapper
public interface DashboardMapper {
    @Select("SELECT COUNT(*) FROM registration WHERE event_id=#{eventId}")
    long participants(Integer eventId);

    @Select("SELECT COUNT(*) FROM team WHERE event_id=#{eventId}")
    long teams(Integer eventId);

    /** 按赛事统计提交过作品的队伍数，跨轮次与版本去重。 */
    @Select(
            "SELECT COUNT(DISTINCT s.team_id) FROM submission s"
                + " JOIN phase p ON p.phase_id=s.phase_id"
                + " JOIN track t ON t.track_id=p.track_id WHERE t.event_id=#{eventId}")
    long submissions(Integer eventId);

    @Select(
            "SELECT s.campus, COUNT(*) AS count FROM registration r LEFT JOIN student s ON"
                + " s.user_id=r.user_id WHERE r.event_id=#{eventId} GROUP BY s.campus")
    List<Map<String, Object>> campuses(Integer eventId);

    @Select(
            "SELECT t.track_id AS trackId,t.name AS trackName,COUNT(DISTINCT s.team_id) AS count"
                + " FROM track t LEFT JOIN phase p ON p.track_id=t.track_id LEFT JOIN submission s"
                + " ON s.phase_id=p.phase_id WHERE t.event_id=#{eventId} GROUP BY t.track_id,t.name"
                + " ORDER BY t.track_id")
    List<Map<String, Object>> trackSubmissions(Integer eventId);

    Map<String, Object> tagCoverage(@Param("eventId") Integer eventId, @Param("ai") boolean ai);

    List<Map<String, Object>> words(@Param("eventId") Integer eventId, @Param("ai") boolean ai);

    Map<String, Object> registrationSummary(Integer eventId);

    List<Map<String, Object>> phases(@Param("eventId") Integer eventId);

    List<Map<String, Object>> judges(
            @Param("eventId") Integer eventId, @Param("now") LocalDateTime now);

    Map<String, Object> workSummary(
            @Param("eventId") Integer eventId, @Param("now") LocalDateTime now);

    IPage<Map<String, Object>> details(
            Page<Map<String, Object>> page,
            @Param("eventId") Integer eventId,
            @Param("kind") DashboardDetailKind kind,
            @Param("phaseId") Integer phaseId,
            @Param("now") LocalDateTime now);
}
