package com.example.community.config;

import com.example.community.util.CommunityContent;
import com.example.community.util.CommunityUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter implements CommunityContent {

    @Override
    public void configure(WebSecurity web) throws Exception {
        // 配置忽略对静态资源的拦截
        web.ignoring().antMatchers("resources/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // controller请求的权限控制
        http.authorizeRequests()
                // 登录才能访问的功能
                .antMatchers(
                        "logout",
                        "/follow",
                        "/unfollow",
                        "/followees/**",
                        "/followers/**",
                        "/like",
                        "/user/setting",
                        "/user/upload",
                        "/user/update/header",
                        "/post/add",
                        "/comment/add/**",
                        "/letter/**",
                        "/notice/**",
                        "/share"

                ).hasAnyAuthority(
                        AUTHORITY_USER,
                        AUTHORITY_ADMIN,
                        AUTHORITY_MODERATOR
                )
                // 版主才能访问的功能
                .antMatchers(
                        "/post/top",
                        "/post/essence"
                ).hasAnyAuthority(
                        AUTHORITY_MODERATOR
                )
                // 管理员才能访问的功能
                .antMatchers(
                        "/post/delete",
                        "/data/**"
                ).hasAnyAuthority(
                        AUTHORITY_ADMIN
                )
                // 设置其他功能呢不需要权限也可以访问
                .anyRequest().permitAll()
                .and().csrf().disable();

        // 权限不足时的处理
        http.exceptionHandling()
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    // 没有权限（即未登录）
                    @Override
                    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
                        String xRequestedWith = request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xRequestedWith)) {
                            // 异步请求
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(CommunityUtil.getJSONString(403, "您还未登录！"));
                        } else {
                            // 同步请求
                            response.sendRedirect(request.getContextPath() + "/login");
                        }
                    }
                })
                .accessDeniedHandler(new AccessDeniedHandler() {
                    // 权限不够
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException, ServletException {
                        String xRequestedWith = request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xRequestedWith)) {
                            // 异步请求
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(CommunityUtil.getJSONString(403, "你没有访问此功能的权限！"));
                        } else {
                            // 同步请求
                            response.sendRedirect(request.getContextPath() + "/denied");
                        }
                    }
                });

        http.logout()
                .logoutUrl("/securityLogout");
    }
}
