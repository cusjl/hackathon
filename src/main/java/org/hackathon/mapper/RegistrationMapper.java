package org.hackathon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.hackathon.data.po.Registration;

@Mapper
public interface RegistrationMapper extends BaseMapper<Registration> {
}
