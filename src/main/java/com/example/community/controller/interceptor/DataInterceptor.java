package com.example.community.controller.interceptor;

import com.example.community.entity.User;
import com.example.community.service.DataService;
import com.example.community.util.HostHodler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class DataInterceptor implements HandlerInterceptor {

    @Autowired
    private DataService dataService;

    @Autowired
    private HostHodler hostHodler;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 记录UV
        String ip = request.getRemoteHost();
        dataService.recordUV(ip);

        // 记录DAU
        User user = hostHodler.getUser();
        if (user != null) {
            dataService.recordDAU(user.getId());
        }

        return true;
    }
}
