package org.hackathon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;
import org.hackathon.data.po.User;

@Mapper
public interface UserMapper extends BaseMapper<User>{
}
