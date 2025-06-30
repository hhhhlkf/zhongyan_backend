package com.gosling.bms.conf;

import com.gosling.bms.response.ResponseResultInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.io.File;


@Configuration
@Slf4j
public class WebMvcConfig extends WebMvcConfigurerAdapter {
    /**
     * 添加自定义拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ResponseResultInterceptor()).addPathPatterns("/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        ApplicationHome h = new ApplicationHome(getClass());
        File jarF = h.getSource();
        File path = new File(jarF.getParentFile().toString());
        String p = path.getAbsolutePath() + "\\static\\";
        p = p.replace('\\', '/');
        log.info(p);
        registry.addResourceHandler("/static/**")//前端url访问的路径，若有访问前缀，在访问时添加即可，这里不需添加。
                .addResourceLocations("file:/D:/zhongyan/backend/src/main/resources/static/images/");//文件存储的真实路径
    }
}

