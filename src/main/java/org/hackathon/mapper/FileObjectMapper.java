package org.hackathon.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.hackathon.data.enums.FileStatus;
import org.hackathon.data.po.FileObject;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FileObjectMapper extends BaseMapper<FileObject> {

    default List<FileObject> selectTimeoutFiles(LocalDateTime deadline) {
        return selectList(new LambdaQueryWrapper<FileObject>()
                .eq(FileObject::getStatus, FileStatus.PENDING)
                .lt(FileObject::getCreateTime, deadline));
    }

    default List<FileObject> selectDeletedFiles(LocalDateTime deadline) {
        return selectList(new LambdaQueryWrapper<FileObject>()
                .eq(FileObject::getStatus, FileStatus.DELETED)
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
