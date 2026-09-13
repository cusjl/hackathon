package org.hackathon.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.apache.ibatis.annotations.*;
import org.hackathon.data.enums.DashboardDetailKind;

import java.time.LocalDateTime;
import java.util.*;

@Mapper
public interface DashboardMapper {
    /** eventId为空时聚合全局；非空时供赛事管理员面板使用。 */
    long participants(@Param("eventId") Integer eventId);

    long teams(@Param("eventId") Integer eventId);

    long submissions(@Param("eventId") Integer eventId);

    List<Map<String, Object>> campuses(@Param("eventId") Integer eventId);

    List<Map<String, Object>> trackSubmissions(@Param("eventId") Integer eventId);

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
