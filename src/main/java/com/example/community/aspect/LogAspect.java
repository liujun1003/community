package com.example.community.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
@Aspect
public class LogAspect {

    private static final Logger logger = LoggerFactory.getLogger(LogAspect.class);

    // 定义切入点
    @Pointcut("execution(* com.example.community.service.*.*(..))")
    public void pointcut() {

    }

    // 定义通知
    @Before("pointcut()")
    public void before(JoinPoint joinPoint) {
        // 日志的格式
        // 用户[ip]在[date]访问了[target]

        // 使用RequestContextHolder获取ServletRequestAttributes，然后获取HttpServletRequest对象
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();

        // ip
        String ip = request.getRemoteHost();

        // 时间
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        // target
        String target = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();

//        logger.info(String.format("用户[%s]，在%s访问了[%s]", ip, date, target));
    }

}
