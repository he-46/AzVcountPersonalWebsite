package com.azv.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.azv.entity.User;
import org.apache.ibatis.annotations.Mapper;
// UserMapper.java —— 继承 BaseMapper 就白得全套 CRUD
@Mapper
public interface UserMapper extends BaseMapper<User> { }


