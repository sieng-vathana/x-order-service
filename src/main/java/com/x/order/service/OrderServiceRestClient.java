package com.x.order.service;

import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.http.HttpClient;
import java.time.Duration;

final class OrderServiceRestClient {
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(8);

    private OrderServiceRestClient() {
    }

    static RestClient create(RestClient.Builder builder, String baseUrl, String servicePath) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return builder.clone()
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
                        String authorization = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                        if (StringUtils.hasText(authorization)) {
                            request.getHeaders().set(HttpHeaders.AUTHORIZATION, authorization);
                        }
                    }
                    return execution.execute(request, body);
                })
                .baseUrl(baseUrl + servicePath)
                .build();
    }
}
