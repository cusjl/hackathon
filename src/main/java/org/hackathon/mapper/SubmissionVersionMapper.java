package org.hackathon.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.hackathon.data.po.SubmissionVersion;

import java.util.List;

@Mapper
public interface SubmissionVersionMapper extends BaseMapper<SubmissionVersion> {

    default List<SubmissionVersion> selectBySubmission(Integer submissionId) {
        return selectList(new LambdaQueryWrapper<SubmissionVersion>()
                .eq(SubmissionVersion::getSubmissionId, submissionId)
                .orderByAsc(SubmissionVersion::getVersionNo));
    }

    default SubmissionVersion selectByVersionNo(Integer submissionId, Integer versionNo) {
        return selectOne(new LambdaQueryWrapper<SubmissionVersion>()
                .eq(SubmissionVersion::getSubmissionId, submissionId)
                .eq(SubmissionVersion::getVersionNo, versionNo));
    }
}
