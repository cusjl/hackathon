package org.hackathon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.hackathon.data.po.StudentRecommendation;

@Mapper
public interface StudentRecommendationMapper extends BaseMapper<StudentRecommendation> {}
