package org.hackathon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.*;
import org.hackathon.data.po.ShowcaseProject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ShowcaseProjectMapper extends BaseMapper<ShowcaseProject> {
    @Select("SELECT * FROM showcase_project WHERE team_id=#{teamId} FOR UPDATE")
    ShowcaseProject lockByTeam(Integer teamId);

    @Select("SELECT team_id FROM team WHERE team_id=#{teamId} FOR UPDATE")
    Integer lockTeam(Integer teamId);

    @Select("SELECT * FROM showcase_project WHERE showcase_id=#{id} FOR UPDATE")
    ShowcaseProject lock(Integer id);

    @Select(
            "SELECT file_id AS fileId, role FROM showcase_project_file WHERE showcase_id=#{id}"
                + " ORDER BY file_id")
    List<Map<String, Object>> files(Integer id);

    @Delete("DELETE FROM showcase_project_file WHERE showcase_id=#{id}")
    void clearFiles(Integer id);

    @Insert(
            "INSERT INTO showcase_project_file(showcase_id,file_id,role)"
                + " VALUES(#{id},#{fileId},#{role})")
    void addFile(Integer id, Long fileId, String role);

    @Select("SELECT EXISTS(SELECT 1 FROM showcase_project_file WHERE file_id=#{fileId})")
    boolean referenced(Long fileId);

    @Select(
            "SELECT EXISTS(SELECT 1 FROM showcase_project_file f JOIN showcase_project s ON"
                + " s.showcase_id=f.showcase_id JOIN event e ON e.event_id=s.event_id WHERE"
                + " f.file_id=#{fileId} AND s.status='PUBLISHED' AND e.live_end<=#{now})")
    boolean publicFile(Long fileId, LocalDateTime now);

    @Insert(
            "INSERT INTO showcase_operation_log(showcase_id,actor_id,action,reason,create_time)"
                + " VALUES(#{id},#{actor},#{action},#{reason},#{now})")
    void log(Integer id, Integer actor, String action, String reason, LocalDateTime now);

    @Select(
            "SELECT actor_id AS actorId, action, reason, create_time AS createTime FROM"
                + " showcase_operation_log WHERE showcase_id=#{id} ORDER BY id")
    List<Map<String, Object>> logs(Integer id);
}
