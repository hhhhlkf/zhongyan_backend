package com.zhongyan.uav.common.response;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

@Component
public class ResponseResultInterceptor implements HandlerInterceptor {
    public static final String RESPONSE_RESULT_ATTRIBUTE = "RESPONSE-RESULT-ANNOTATION";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Class<?> beanType = handlerMethod.getBeanType();
        Method method = handlerMethod.getMethod();
        if (beanType.isAnnotationPresent(ResponseResult.class)) {
            request.setAttribute(RESPONSE_RESULT_ATTRIBUTE, beanType.getAnnotation(ResponseResult.class));
        } else if (method.isAnnotationPresent(ResponseResult.class)) {
            request.setAttribute(RESPONSE_RESULT_ATTRIBUTE, method.getAnnotation(ResponseResult.class));
        }
        return true;
    }
}

