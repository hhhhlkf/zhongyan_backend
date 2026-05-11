package com.zhongyan.uav;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

class ZhongyanUavApplicationTests {

    @Test
    void contextLoads() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(ZhongyanUavApplication.class)
                .profiles("test")
                .web(WebApplicationType.NONE)
                .run()) {
        }
    }
}
