package com.alibaba.nacos.example.spring.cloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * @author wuqunqun
 */
@SpringBootApplication
@EnableDiscoveryClient
public class Application {
    
    
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
    
}
