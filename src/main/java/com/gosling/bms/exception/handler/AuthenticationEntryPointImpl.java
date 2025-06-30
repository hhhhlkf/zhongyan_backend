package com.gosling.bms.exception.handler;

import com.alibaba.fastjson.JSON;
import com.gosling.bms.response.ErrorResult;
import com.gosling.bms.utils.WebUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.gosling.bms.utils.enums.ResultCode.AUTHENTICATION_ERROR;

/**
 * @ClassName:AuthenticationEntryPointImpl
 * @Description: TODO
 * @Author:Dazz1e
 * @Date:2022/5/23 下午 4:20
 * Version V1.0
 */
@Component
@Slf4j
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        ErrorResult result = ErrorResult.error(AUTHENTICATION_ERROR, "登录状态异常");
        String json = JSON.toJSONString(result);
        WebUtil.renderString(response, json);
    }


}
