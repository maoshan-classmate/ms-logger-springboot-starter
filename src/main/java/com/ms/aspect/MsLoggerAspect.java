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
     * 获取IP地址
     *
     * @param request 请求
     * @return IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理 X-Forwarded-For 头部可能出现的多个 IP 地址的情况
        if (ip != null && ip.indexOf(",") > 0) {
            ip = ip.split(",")[0];
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
