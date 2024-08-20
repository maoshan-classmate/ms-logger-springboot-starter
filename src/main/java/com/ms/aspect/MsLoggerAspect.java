package com.ms.aspect;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.json.JSONUtil;
import com.ms.config.MsLoggerProperties;
import com.ms.dto.MsLogger;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.support.MultipartFilter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

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

    private static final TimeInterval TIMER = DateUtil.timer();


    @Resource
    private MsLogger msLogger;


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
            msLogger.setIpAddress(getClientIp(request));
            msLogger.setApiUrl(request.getRequestURL().toString());
        }
    }

    @Around("pointcut()")
    public Object recordSysLogger(ProceedingJoinPoint joinPoint) throws Throwable {
        if (msLoggerProperties.isEnable()) {
            MsLogger msLogger = buildSysLogger(joinPoint);
            MsLoggerAbstractStrategy msLoggerStrategy = MsLoggerFactory.getMsLoggerStrategy(msLoggerProperties.getLoggerStrategy());
            Object[] args = joinPoint.getArgs();
            Object result;
            try {
                TIMER.start();
                result = joinPoint.proceed(args);
                long cost = TIMER.intervalMs();
                msLogger.setCost(cost);
                msLoggerStrategy.doLog(msLogger,joinPoint,JSONUtil.toJsonStr(result),cost);
            } catch (Throwable e) {
                LOGGER.error("记录日志异常：{}", e.getMessage());
                throw e;
            }
            return result;
        } else {
            return joinPoint.proceed();
        }
    }

    /**
     * 构建日志对象
     * @param joinPoint 切点
     * @return 日志对象
     */
    protected MsLogger buildSysLogger(ProceedingJoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        com.ms.annotation.MsLogger msLogger = method.getAnnotation(com.ms.annotation.MsLogger.class);
        Class<?> declaringClass = method.getDeclaringClass();
        this.msLogger.setMethodName(declaringClass.getSimpleName() + "." + method.getName());
        if (msLogger != null) {
            this.msLogger.setLogDesc(msLogger.desc());
        }else {
            com.ms.annotation.MsLogger annotation = declaringClass.getAnnotation(com.ms.annotation.MsLogger.class);
            if (annotation != null){
                this.msLogger.setLogDesc(annotation.desc());
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
            this.msLogger.setParams(JSONUtil.toJsonStr(paramMap));
            return this.msLogger;
        } catch (Exception e) {
            LOGGER.error("构建入参异常：{}", e.getMessage());
        }
        return this.msLogger;
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
     * @param request HTTP请求对象
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

    /**
     * 过滤参数
     * @param args 参数数组
     * @return 过滤后的参数集合
     */
    private List<Object> filterArgs(Object[] args) {
        return Arrays.stream(args).filter(object -> !(object instanceof MultipartFilter)
                && !(object instanceof HttpServletRequest)
                && !(object instanceof HttpServletResponse)
        ).collect(Collectors.toList());
    }




}
