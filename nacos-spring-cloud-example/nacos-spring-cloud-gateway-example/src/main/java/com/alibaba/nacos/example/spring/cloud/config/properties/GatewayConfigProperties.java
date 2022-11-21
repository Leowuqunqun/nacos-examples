package com.alibaba.nacos.example.spring.cloud.config.properties;

import com.alibaba.nacos.example.spring.cloud.constant.CommonConstant;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author wuqunqun
 * @since 2022-11-14
 */
@Data
@ConfigurationProperties(prefix = GatewayConfigProperties.ROOT_PREFIX)
public class GatewayConfigProperties {
    
    public static final String ROOT_PREFIX = CommonConstant.PACKAGE_NAME + ".gateway";
    
    private CorsAllowOriginProperties cors;
    
    @Data
    public class CorsAllowOriginProperties {
        
        private List<String> origins;
        
        private String method;
        
        private String headers;
        
        private boolean enabled;
        
    }
    
    
}
