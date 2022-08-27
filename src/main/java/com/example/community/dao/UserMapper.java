package com.example.community.dao;

import com.example.community.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    User selectUserById(int id);

    User selectUserByName(String username);

    User selectUserByEmail(String email);

    int insertUser(User user);

    int deleteUserById(int id);

    int updatePassword(@Param("id") int id, @Param("password") String password);

    int updateType(@Param("id") int id, @Param("type") int type);

    int updateStatus(@Param("id") int id, @Param("status") int status);

    int updateHeaderUrl(@Param("id") int id, @Param("headerUrl") String headerUrl);

}
