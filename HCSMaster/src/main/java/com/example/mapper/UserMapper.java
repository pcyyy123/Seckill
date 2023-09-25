package com.example.mapper;

import com.example.pojo.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
* @author pcy
* @description 针对表【t_user】的数据库操作Mapper
* @createDate 2023-09-14 16:11:43
* @Entity com.example.pojo.User
*/
@Mapper
public interface UserMapper extends BaseMapper<User> {

}




