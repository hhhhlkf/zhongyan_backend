package com.gosling.bms.exception.handler;

import com.alibaba.fastjson.JSON;
import com.gosling.bms.response.ErrorResult;
import com.gosling.bms.utils.enums.ResultCode;
import com.gosling.bms.utils.WebUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@Component
@Slf4j
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {


    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        ErrorResult result = ErrorResult.error(ResultCode.ACCESS_DENIED, "权限不足");
        String json = JSON.toJSONString(result);
        WebUtil.renderString(response, json);
    }
}
