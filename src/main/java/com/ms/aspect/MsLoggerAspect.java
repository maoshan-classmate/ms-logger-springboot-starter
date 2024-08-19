package com.ms.aspect;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.json.JSONUtil;
import com.ms.annotation.MsLogger;
import com.ms.config.MsLoggerProperties;
import com.ms.dto.SysLogger;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
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

    private final MsLoggerProperties msLoggerProperties = MsLoggerProperties.getInstance();

    private static final Logger LOGGER = LoggerFactory.getLogger(MsLoggerAspect.class);

    private static final ServletRequestAttributes REQUEST_ATTRIBUTES = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

    private static final TimeInterval TIMER = DateUtil.timer();

    private final SysLogger sysLogger = new SysLogger();

    /**
     * TODO 类级别的切点
     */
//    @Pointcut("execution()")
//    public void matchLogPointcut(){
//
//    }

    @Pointcut("@annotation(com.ms.annotation.MsLogger) ")
    public void pointcut() {
    }

    @Before("pointcut()")
    public void doBefore(JoinPoint joinPoint) {
        if (msLoggerProperties.isEnable()) {
            HttpServletRequest request = REQUEST_ATTRIBUTES.getRequest();
            sysLogger.setIpAddress(getClientIp(request));
            sysLogger.setApiUrl(request.getRequestURL().toString());
        }
    }

    @Around("pointcut()")
    public Object recordSysLogger(ProceedingJoinPoint joinPoint) throws Throwable {
        if (msLoggerProperties.isEnable()) {
            SysLogger sysLogger = buildSysLogger(joinPoint);
            try {
                TIMER.start();
                Object proceed = joinPoint.proceed();
                sysLogger.setCost(TIMER.intervalMs());
                return proceed;
            } finally {
                LOGGER.info(JSONUtil.toJsonStr(sysLogger));
            }
        } else {
            return joinPoint.proceed();
        }
    }

    /**
     * 构建日志对象
     * @param joinPoint 切点
     * @return 日志对象
     */
    protected SysLogger buildSysLogger(ProceedingJoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        MsLogger msLogger = method.getAnnotation(MsLogger.class);
        sysLogger.setMethodName(method.getDeclaringClass().getSimpleName() + "." + method.getName());
        if (msLogger != null) {
            sysLogger.setLogDesc(msLogger.desc());
        }
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
            LOGGER.error("构建入参异常：{}", e.getMessage());
        }
        return sysLogger;
    }

    /**
     * 获取IP地址
     *
     * @param request 请求
     * @return IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        // 一般都会有代理转发，真实的ip会放在X-Forwarded-For
        String xff = request.getHeader("X-Forwarded-For");
        if (xff == null) {
            return request.getRemoteAddr();
        } else {
            return xff.contains(",") ? xff.split(",")[0] : xff;
        }
    }

    /**
     * 获取匹配的Url
     * @return url
     */
//    private String getMatchUrl(){
//        return msLoggerProperties.getServerPath();
//    }


}
