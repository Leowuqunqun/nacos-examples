package com.alibaba.nacos.example.spring.cloud.config;

import com.alibaba.nacos.example.spring.cloud.config.properties.GatewayConfigProperties;
import com.alibaba.nacos.example.spring.cloud.filter.LogGlobalFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({GatewayConfigProperties.class})
public class GatewayAutoConfiguration {
    
    
    //region GlobalFilter
    
    @Bean
    public GlobalFilter blackListGlobalFilter(GatewayConfigProperties gatewayConfigProperties) {
        return new LogGlobalFilter(gatewayConfigProperties);
    }
    
    //endregion
}
