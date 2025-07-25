package com.dsb.sqlexecutor.aspect;


import com.dsb.sqlexecutor.repository.SqlExecutorRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Enumeration;

@Aspect
@Component
public class ApiLoggingAspect {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // 定义切点：拦截所有Controller类的public方法
    @Pointcut("execution(public * com.dsb.sqlexecutor.controller.*.*(..))")
    public void controllerPointcut() {}

    @Autowired
    private SqlExecutorRepository sqlExecutorRepository;

    // 前置通知：在方法执行前执行
    @Before("controllerPointcut()")
    public void beforeAdvice(JoinPoint joinPoint) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            // 记录请求信息
            log.info("===== 请求开始 =====");
            log.info("请求URL: {}", request.getRequestURL().toString());
            log.info("请求方法: {}", request.getMethod());
            log.info("请求IP: {}", request.getRemoteAddr());
            log.info("请求类方法: {}", joinPoint.getSignature());
            log.info("请求参数: {}", Arrays.toString(joinPoint.getArgs()));

            // 记录请求头
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                log.info("请求头 - {}: {}", headerName, request.getHeader(headerName));
            }
        }
    }

    // 后置返回通知：在方法正常返回后执行
    @AfterReturning(pointcut = "controllerPointcut()", returning = "result")
    public void afterReturningAdvice(JoinPoint joinPoint, Object result) {
        log.info("===== 请求返回 =====");
        log.info("返回结果: {}", result);
    }

    // 后置异常通知：在方法抛出异常后执行
    @AfterThrowing(pointcut = "controllerPointcut()", throwing = "ex")
    public void afterThrowingAdvice(JoinPoint joinPoint, Throwable ex) {
        log.error("===== 请求异常 =====");
        log.error("异常信息: {}", ex.getMessage(), ex);
    }

    // 后置最终通知：无论方法是否正常执行完毕都会执行
    @After("controllerPointcut()")
    public void afterAdvice() {
        sqlExecutorRepository.getJdbcTemplateThreadLocal().remove();
        log.info("===== 请求结束 =====");
    }
}
