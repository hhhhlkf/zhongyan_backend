package com.zhongyan.uav;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class ZhongyanUavApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZhongyanUavApplication.class, args);
    }
}
