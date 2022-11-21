package com.alibaba.nacos.example.spring.cloud.constant;

import org.springframework.core.Ordered;

/**
 * @author wuqunqun
 * @since 2022-11-14
 */
public class FilterConstant {
    
    // 集中过滤器顺序，方便一眼看清请求链路 | Centralized filter order, easy to see the request link at a glance
    
    public static final Integer LOG_GLOBAL_FILTER_ORDERED = Ordered.HIGHEST_PRECEDENCE;


}
