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
import org.springframework.web.multipart.support.MultipartFilter;

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

    private final SysLogger sysLogger = new SysLogger();

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
            sysLogger.setIpAddress(getClientIp(request));
            sysLogger.setApiUrl(request.getRequestURL().toString());
        }
    }

    @Around("pointcut()")
    public Object recordSysLogger(ProceedingJoinPoint joinPoint) throws Throwable {
        if (msLoggerProperties.isEnable()) {
            SysLogger sysLogger = buildSysLogger(joinPoint);
            Object[] args = joinPoint.getArgs();
            Object result = null;
            try {
                TIMER.start();
                result = joinPoint.proceed(args);
                long cost = TIMER.intervalMs();
                sysLogger.setCost(cost);
                printLog(sysLogger,joinPoint,JSONUtil.toJsonStr(result),cost);
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

    /**
     * 日志输出
     * @param sysLogger 日志对象
     * @param joinPoint 切点
     * @param outParams 返回参数
     * @param cost 花费时间
     */
    private void printLog(SysLogger sysLogger,JoinPoint joinPoint,String outParams,long cost) {
        log.info("\n\r=======================================\n\r" +
                        "日志描述:{} \n\r" +
                        "请求地址:{} \n\r" +
                        "请求接口URL:{} \n\r"+
                        "请求方式:{} \n\r" +
                        "请求类方法:{} \n\r" +
                        "请求方法参数:{} \n\r" +
                        "返回报文:{} \n\r" +
                        "处理耗时:{} ms \n\r" +
                        "=======================================\n\r",
                sysLogger.getLogDesc(),
                sysLogger.getIpAddress(),
                sysLogger.getApiUrl(),
                sysLogger.getMethodName(),
                joinPoint.getSignature(),
                JSONUtil.toJsonStr(filterArgs(joinPoint.getArgs())),
                outParams,
                cost
        );
    }


}
