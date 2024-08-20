package com.ms.aspect;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.json.JSONUtil;
import com.ms.handler.MsLoggerHandler;
import com.ms.annotation.MsLogger;
import com.ms.config.MsLoggerProperties;
import com.ms.dto.Logger;
import com.ms.pattern.factory.MsLoggerFactory;
import com.ms.pattern.strategy.MsLoggerAbstractStrategy;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
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

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MsLoggerAspect.class);

    private static final TimeInterval TIMER = DateUtil.timer();

    private final ApplicationContext applicationContext;

    @Resource
    private Logger logger;

    public MsLoggerAspect(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }


    private static final String[] HEADERS = {
            "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"
    };


    @Pointcut("@annotation(com.ms.annotation.MsLogger) || @within(com.ms.annotation.MsLogger) ")
    public void pointcut() {
    }

    @Before("pointcut()")
    public void doBefore(JoinPoint joinPoint) {
        if (msLoggerProperties.isEnable()) {
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = requestAttributes.getRequest();
            logger.setIpAddress(getClientIp(request));
            logger.setApiUrl(request.getRequestURL().toString());
        }
    }

    @Around("pointcut()")
    public Object recordSysLogger(ProceedingJoinPoint joinPoint) throws Throwable {
        Class<? extends MsLoggerHandler> annotationHandler = getHandlerClass(joinPoint);
        MsLoggerHandler loggerHandler = null;
        if (annotationHandler != null && !annotationHandler.isInterface()) {
            loggerHandler = applicationContext.getBean(annotationHandler);
        }
        Object[] args = joinPoint.getArgs();
        if (msLoggerProperties.isEnable()) {
            Logger logger = buildSysLogger(joinPoint);
            MsLoggerAbstractStrategy msLoggerStrategy = MsLoggerFactory.getMsLoggerStrategy(msLoggerProperties.getLoggerStrategy());
            Object result;
            try {
                TIMER.start();
                result = joinPoint.proceed(args);
                long cost = TIMER.intervalMs();
                logger.setCost(cost);
                msLoggerStrategy.doLog(logger, joinPoint, JSONUtil.toJsonStr(result), cost);
            } catch (Throwable e) {
                LOGGER.error("记录日志异常：{}", e.getMessage());
                throw e;
            }
        }
        if (loggerHandler != null) {
            loggerHandler.handleLogger();
        }
        return joinPoint.proceed(args);
    }

    /**
     * 获取日志增强处理器
     *
     * @param joinPoint 切点
     * @return 日志增强处理器
     */
    private Class<? extends MsLoggerHandler> getHandlerClass(ProceedingJoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        MsLogger msLoggerAnnotation = method.getAnnotation(MsLogger.class);
        Class<? extends MsLoggerHandler> annotationHandler = msLoggerAnnotation.handler();
        if (annotationHandler == null) {
            Class<?> declaringClass = method.getDeclaringClass();
            MsLogger annotation = declaringClass.getAnnotation(MsLogger.class);
            annotationHandler = annotation.handler();
        }
        return annotationHandler;
    }

    /**
     * 构建日志对象
     *
     * @param joinPoint 切点
     * @return 日志对象
     */
    protected Logger buildSysLogger(ProceedingJoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        MsLogger logger = method.getAnnotation(MsLogger.class);
        Class<?> declaringClass = method.getDeclaringClass();
        this.logger.setMethodName(declaringClass.getSimpleName() + "." + method.getName());
        if (logger != null) {
            this.logger.setLogDesc(logger.desc());
        } else {
            MsLogger annotation = declaringClass.getAnnotation(MsLogger.class);
            if (annotation != null) {
                this.logger.setLogDesc(annotation.desc());
            }
        }
        try {
            // 处理入参
            Parameter[] parameters = methodSignature.getMethod().getParameters();
            HashMap<String, Object> paramMap = new HashMap<>();
            Object[] args = joinPoint.getArgs();
            for (int i = 0; i < parameters.length; i++) {
                com.ms.annotation.MsLogger auditLog = parameters[i].getAnnotation(com.ms.annotation.MsLogger.class);
                if (auditLog != null) {
                    continue;
                }
                String name = parameters[i].getName();
                paramMap.put(name, args[i]);
            }
            this.logger.setParams(JSONUtil.toJsonStr(paramMap));
            return this.logger;
        } catch (Exception e) {
            LOGGER.error("构建入参异常：{}", e.getMessage());
        }
        return this.logger;
    }

    /**
     * 获取客户端的真实IP地址。
     * 首先尝试从各种代理头部获取IP，如果都失败，则返回请求的远程地址。
     *
     * @param request HTTP请求对象
     * @return 客户端的IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        // 循环尝试获取头部中的IP地址
        for (String header : HEADERS) {
            String ip = getHeaderOrUnknown(request, header);
            if (!"unknown".equalsIgnoreCase(ip)) {
                return processIp(ip);
            }
        }
        // 如果所有头部都没有有效IP，则返回请求的远程地址
        return request.getRemoteAddr();
    }

    /**
     * 从请求头部获取值，如果没有则返回 "unknown"。
     *
     * @param request    HTTP请求对象
     * @param headerName 头部名称
     * @return 头部的值或 "unknown"
     */
    private String getHeaderOrUnknown(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        // 如果头部值不为空且不为 ""，则返回头部值，否则返回 "unknown"
        return (value != null && !value.isEmpty()) ? value : "unknown";
    }

    /**
     * 处理从头部获取的IP字符串。
     * 如果IP字符串中包含多个IP地址（用逗号分隔），则只返回第一个IP地址。
     *
     * @param ip 从头部获取的原始IP字符串
     * @return 处理后的IP地址
     */
    private String processIp(String ip) {
        if (ip != null && ip.contains(",")) {
            String[] parts = ip.split(",");
            // 返回第一个IP地址，并去除前后空白字符
            return parts[0].trim();
        }
        return ip;
    }

    private boolean isInterface(Class<? extends MsLoggerHandler> clazz) {
        return clazz.isInterface();
    }


}
