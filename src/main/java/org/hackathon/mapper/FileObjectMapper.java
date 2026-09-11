package org.hackathon.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.hackathon.data.enums.FileStatus;
import org.hackathon.data.po.FileObject;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FileObjectMapper extends BaseMapper<FileObject> {

    @Select("SELECT * FROM file_object WHERE file_id=#{fileId} FOR UPDATE")
    FileObject lockById(Long fileId);

    default List<FileObject> selectTrackAttachments(Integer trackId) {
        return selectList(new LambdaQueryWrapper<FileObject>()
                .eq(FileObject::getTrackId, trackId)
                .eq(FileObject::getScope, org.hackathon.data.enums.FileScope.TRACK_ATTACHMENT)
                .eq(FileObject::getStatus, FileStatus.READY)
                .orderByDesc(FileObject::getCreateTime));
    }

    default List<FileObject> selectTimeoutFiles(LocalDateTime deadline) {
        return selectList(new LambdaQueryWrapper<FileObject>()
                .eq(FileObject::getStatus, FileStatus.PENDING)
                .notExists("SELECT 1 FROM showcase_project_file f WHERE f.file_id=file_object.file_id")
                .lt(FileObject::getCreateTime, deadline));
    }

    default List<FileObject> selectDeletedFiles(LocalDateTime deadline) {
        return selectList(new LambdaQueryWrapper<FileObject>()
                .eq(FileObject::getStatus, FileStatus.DELETED)
                .notExists("SELECT 1 FROM showcase_project_file f WHERE f.file_id=file_object.file_id")
                .lt(FileObject::getUpdateTime, deadline));
    }

    default List<FileObject> selectByTeamPhase(Integer teamId, Integer phaseId) {
        return selectList(new LambdaQueryWrapper<FileObject>()
                .eq(FileObject::getTeamId, teamId)
                .eq(FileObject::getPhaseId, phaseId)
                .eq(FileObject::getStatus, FileStatus.READY)
                .orderByDesc(FileObject::getCreateTime));
    }
}
