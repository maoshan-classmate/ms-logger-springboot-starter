package com.ms.aspect;

import cn.hutool.json.JSONUtil;
import com.ms.annotation.MsLogger;
import com.ms.dto.SysLogger;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;

/**
 * @author maoshan-classmate
 * @date 2024-08-14
 */
@Slf4j
@Component
@Aspect
public class MsLoggerAspect {


    private static final Logger logger = LoggerFactory.getLogger(MsLoggerAspect.class);

//    private static final TimeInterval timer = DateUtil.timer();

    @Pointcut(value = "@annotation(com.ms.annotation.MsLogger)")
    public void pointcut() {
    }

    @Around("pointcut()")
    public Object recordSysLogger(ProceedingJoinPoint joinPoint) throws Throwable {
        SysLogger sysLogger = buildSysLogger(joinPoint);
        try {
             return joinPoint.proceed();
        } finally {
            logger.info(JSONUtil.toJsonStr(sysLogger));
        }
    }

    protected SysLogger buildSysLogger(ProceedingJoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        MsLogger msLogger = method.getAnnotation(MsLogger.class);
        SysLogger sysLogger = new SysLogger();
        sysLogger.setIpAddress("127.0.0.1");
        sysLogger.setLogDesc(msLogger.desc());
        sysLogger.setMethodName(method.getDeclaringClass().getSimpleName() + "." + method.getName());
        try {
            // 处理入参
            Parameter[] parameters = methodSignature.getMethod().getParameters();
            HashMap<String, Object> paramMap = new HashMap<>();
            Object[] args = joinPoint.getArgs();
            for (int i = 0; i < parameters.length; i++) {
                MsLogger auditLog = parameters[i].getAnnotation(MsLogger.class);
                if (auditLog != null) {
                    continue;
                }
                String name = parameters[i].getName();
                paramMap.put(name, args[i]);
            }
            sysLogger.setParams(JSONUtil.toJsonStr(paramMap));
            return sysLogger;
        } catch (Exception e) {
            logger.error("构建入参异常：{}", e.getMessage());
        }
        return sysLogger;
    }
}
