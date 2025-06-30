package com.gosling.bms.filter;

import com.gosling.bms.exception.BaseException;
import com.gosling.bms.response.ResponseResult;
import com.gosling.bms.utils.JWTUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @ClassName:JwtAuthenticationTokenFilter
 * @Description: TODO
 * @Author:Dazz1e
 * @Date:2022/11/26 下午 5:19
 * Version V1.0
 */
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {
    @Autowired
    private JWTUtil jwtUtil;

    @Override
    @ResponseResult
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String method = request.getMethod();
        if (method.equals("OPTIONS")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = request.getHeader("token");
//        logger.info(token);
        String path = request.getServletPath();
        if (path.equals("/users/login") || path.equals("/user/register")) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!StringUtils.hasText(token)) {
            // 白名单放行
              filterChain.doFilter(request, response);
            return;
        }
        String userId;
        try {
            //logger.info(token);
            Claims claims = JWTUtil.parseJWT(token);
            userId = claims.getSubject();
        } catch (Exception e) {
            throw new BaseException("token非法");
        }
        //查询redis

//        String redisKey = "Employee_login:" + userId;
//        LoginEmployee loginEmployee = (LoginEmployee) redisUtil.get(redisKey);
//        if (Objects.isNull(loginEmployee)) {
//            throw new BaseException("用户未登录");
//        }
//
//
//        //存入UserDetailsServiceImpl
//        //权限封装进Authentication
//        UsernamePasswordAuthenticationToken authenticationToken =
//                new UsernamePasswordAuthenticationToken(
//                        loginEmployee, null,
//                        loginEmployee.getAuthorities()
//                );
//        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
//        filterChain.doFilter(request, response);
    }


}
