package com.alibaba.nacos.example.spring.cloud.route;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

@Slf4j
@Component
public class DynamicGatewayRouteConfig implements ApplicationEventPublisherAware {
    
    
    @Autowired
    private NacosConfigManager nacosConfigManager;
    
    private RouteDefinitionWriter routeDefinitionWriter;
    
    private final long timeoutMs = 5000;
    
    @Autowired
    public void setRouteDefinitionWriter(RouteDefinitionWriter routeDefinitionWriter) {
        this.routeDefinitionWriter = routeDefinitionWriter;
    }
    
    private ApplicationEventPublisher applicationEventPublisher;
    
    private static final List<String> ROUTES = new ArrayList<String>();
    
    @PostConstruct
    public void dynamicRouteByNacosListener() {
        try {
            ConfigService configService = nacosConfigManager.getConfigService();
            // 程序首次启动, 并加载初始化路由配置
            String initConfigInfo = configService.getConfig(dataId, group, timeoutMs);
            batchAddOrUpdateRouteAndPublish(initConfigInfo);
            configService.addListener(dataId, group, new Listener() {
                @Override
                public void receiveConfigInfo(String configInfo) {
                    batchAddOrUpdateRouteAndPublish(configInfo);
                }
                
                @Override
                public Executor getExecutor() {
                    return null;
                }
            });
        } catch (NacosException e) {
            e.printStackTrace();
        }
        
    }
    
    /**
     * 清空所有路由
     */
    private void clearRoute() {
        for (String id : ROUTES) {
            this.routeDefinitionWriter.delete(Mono.just(id)).subscribe();
        }
        ROUTES.clear();
    }
    
    /**
     * 添加单条路由信息
     *
     * @param definition RouteDefinition
     */
    private void addRoute(RouteDefinition definition) {
        routeDefinitionWriter.save(Mono.just(definition)).subscribe();
        ROUTES.add(definition.getId());
    }
    
    /**
     * 批量添加或更新路由，及发布 路由
     *
     * @param configInfo 配置文件字符串, 必须为json array格式
     */
    private void batchAddOrUpdateRouteAndPublish(String configInfo) {
        try {
            clearRoute();
            List<RouteDefinition> gatewayRouteDefinitions = JSONObject.parseArray(configInfo, RouteDefinition.class);
            for (RouteDefinition routeDefinition : gatewayRouteDefinitions) {
                addRoute(routeDefinition);
            }
            publish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void publish() {
        this.applicationEventPublisher.publishEvent(new RefreshRoutesEvent(this.routeDefinitionWriter));
    }
    
    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
    
    
}