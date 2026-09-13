package org.hackathon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.*;
import org.hackathon.data.po.GlobalDashboardConfig;

@Mapper
public interface GlobalDashboardConfigMapper extends BaseMapper<GlobalDashboardConfig> {
    @Select("SELECT config_id FROM global_dashboard_config WHERE config_id=1 FOR UPDATE")
    Integer lockConfig();
}
