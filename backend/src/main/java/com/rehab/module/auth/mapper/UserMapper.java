package com.rehab.module.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rehab.module.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
