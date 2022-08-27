package com.example.community.controller.interceptor;

import com.example.community.annotation.LoginRequired;
import com.example.community.util.HostHodler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

@Component
@Deprecated
public class LoginRequiredInterceptor implements HandlerInterceptor {

    @Autowired
    private HostHodler hostHodler;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 可以利用handler判断该方法是否有我们自定义的@LoginRequired注解
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            //获取Method反射对象
            Method method = handlerMethod.getMethod();
            LoginRequired loginRequired = method.getAnnotation(LoginRequired.class);
            if (loginRequired != null && hostHodler.getUser() == null) {
                // 根据异步请求和同步请求，做重定向的处理
                String xRequestedWith = request.getHeader("x-requested-with");
                if ("XMLHttpRequest".equals(xRequestedWith)) {
                    response.setHeader("LOGIN_STATUS", "no");
                    response.setHeader("REDIRCCT_URL", "/login");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                } else {
                    response.sendRedirect(request.getContextPath() + "/login");
                }
                return false;
            }
        }
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }
}
