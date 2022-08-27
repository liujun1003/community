package com.example.community.dao;

import com.example.community.entity.LoginTicket;
import org.apache.ibatis.annotations.*;

@Mapper
@Deprecated
public interface LoginTicketMapper {

    @Insert(
            "insert into login_ticket (user_id, ticket, status, expired) " +
                    "values (#{userId}, #{ticket}, #{status}, #{expired})"
    )
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertLoginTicket(LoginTicket loginTicket);

    @Update(
            "update login_ticket " +
                    "set status = #{status} " +
                    "where ticket = #{ticket} "
    )
    int updateStatusByTicket(@Param("ticket") String ticket, @Param("status") int status);

    @Select(
            "select id, user_id, ticket, status, expired " +
                    "from login_ticket " +
                    "where ticket = #{ticket}"
    )
    LoginTicket selectLoginTicketByTicket(String ticket);

}
