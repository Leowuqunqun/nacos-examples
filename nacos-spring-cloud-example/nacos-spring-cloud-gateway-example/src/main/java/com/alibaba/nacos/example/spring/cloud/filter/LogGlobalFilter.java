package com.alibaba.nacos.example.spring.cloud.filter;

import com.alibaba.nacos.example.spring.cloud.config.properties.GatewayConfigProperties;
import com.alibaba.nacos.example.spring.cloud.constant.FilterConstant;
import com.alibaba.nacos.example.spring.cloud.util.ContentTypeUtil;
import io.netty.buffer.UnpooledByteBufAllocator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @Author wuqunqun
 * @Date 2022-11-08
 * @Description Log record
 */
@Component
@Slf4j
public class LogGlobalFilter implements GlobalFilter, Ordered {
    
    
    public LogGlobalFilter(GatewayConfigProperties gatewayConfigProperties) {
    }
    
    @Override
    public int getOrder() {
        return FilterConstant.LOG_GLOBAL_FILTER_ORDERED;
    }
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        try {
            ServerHttpRequest request = exchange.getRequest();
            ServerRequest serverRequest = ServerRequest.create(exchange,
                    HandlerStrategies.withDefaults().messageReaders());
            URI requestUri = request.getURI();
            String uriQuery = requestUri.getQuery();
            HttpHeaders headers = request.getHeaders();
            MediaType mediaType = headers.getContentType();
            String method = request.getMethodValue().toUpperCase();
            
            final AtomicReference<String> requestBody = new AtomicReference<>();
            // 如果是通过前端直接上传OSS不走网关 可以删除 | If you upload the OSS directly through the front end without going through the gateway, you can delete it
            if (Objects.nonNull(mediaType) && ContentTypeUtil.isUploadFile(mediaType)) {
                requestBody.set("upload file");
                return chain.filter(exchange);
            } else {
                if (method.equals(HttpMethod.GET.toString())) {
                    if (StringUtils.isNotBlank(uriQuery)) {
                        requestBody.set(uriQuery);
                    }
                } else if (headers.getContentLength() > 0) {
                    return serverRequest.bodyToMono(String.class).flatMap(reqBody -> {
                        requestBody.set(reqBody);
                        // 重写原始请求 | rewrite the original request
                        ServerHttpRequestDecorator requestDecorator = new ServerHttpRequestDecorator(
                                exchange.getRequest()) {
                            @Override
                            public HttpHeaders getHeaders() {
                                HttpHeaders httpHeaders = new HttpHeaders();
                                httpHeaders.putAll(super.getHeaders());
                                return httpHeaders;
                            }
                            
                            @Override
                            public Flux<DataBuffer> getBody() {
                                NettyDataBufferFactory nettyDataBufferFactory = new NettyDataBufferFactory(
                                        new UnpooledByteBufAllocator(false));
                                DataBuffer bodyDataBuffer = nettyDataBufferFactory.wrap(reqBody.getBytes());
                                return Flux.just(bodyDataBuffer);
                            }
                        };
                        ServerHttpResponseDecorator responseDecorator = getServerHttpResponseDecorator(exchange,
                                requestBody);
                        return chain.filter(
                                exchange.mutate().request(requestDecorator).response(responseDecorator).build());
                    });
                }
                ServerHttpResponseDecorator decoratedResponse = getServerHttpResponseDecorator(exchange, requestBody);
                return chain.filter(exchange.mutate().response(decoratedResponse).build());
            }
            
        } catch (Exception e) {
            log.error("LogGlobalFilter is error ", e);
            return chain.filter(exchange);
        }
    }
    
    private ServerHttpResponseDecorator getServerHttpResponseDecorator(ServerWebExchange exchange,
            AtomicReference<String> requestBody) {
        // get response result
        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();
        HttpStatus httpStatus = originalResponse.getStatusCode();
        ServerHttpRequest request = exchange.getRequest();
        URI requestUri = request.getURI();
        String uriQuery = requestUri.getQuery();
        String url = requestUri.getPath() + (StringUtils.isNotBlank(uriQuery) ? "?" + uriQuery : "");
        HttpHeaders headers = request.getHeaders();
        String headerStr = HttpHeaders.formatHeaders(headers);
        String method = request.getMethodValue().toUpperCase();
        return new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    Flux<? extends DataBuffer> fluxBody = Flux.from(body);
                    return super.writeWith(fluxBody.buffer().map(dataBuffers -> {
                        DataBuffer join = bufferFactory.join(dataBuffers);
                        byte[] content = new byte[join.readableByteCount()];
                        join.read(content);
                        DataBufferUtils.release(join);
                        Charset charset = ContentTypeUtil.getMediaTypeCharset(originalResponse.getHeaders().getContentType());
                        String responseBody = new String(content, charset);
                        
                        log.info("url:{},method:{},header:{},requestBody:{},responseBody:{},status:{}", url,
                                method, headerStr, requestBody.get(), responseBody, httpStatus);
                        return bufferFactory.wrap(content);
                    }));
                }
                return super.writeWith(body);
            }
        };
    }
    
}