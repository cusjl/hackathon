package org.hackathon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.*;
import org.hackathon.data.po.DashboardConfig;

@Mapper
public interface DashboardConfigMapper extends BaseMapper<DashboardConfig> {
    @Select("SELECT event_id FROM event WHERE event_id=#{eventId} FOR UPDATE")
    Integer lockEvent(Integer eventId);
}
