package com.smartgym.gateway.config;

import com.smartgym.gateway.service.TokenStore;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import reactor.core.publisher.Mono;

@Component
public class ExternalAuthFilter implements GlobalFilter, Ordered {

    private final TokenStore tokenStore;

    public ExternalAuthFilter(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        //if (path.startsWith("/auth") || path.startsWith("/analytics") || path.startsWith("/actuator")  ||  path.startsWith("/embedded") || path.contains("eureka") || path.startsWith("/ticketing")
          //      || path.startsWith("/parking") || path.startsWith("/payment")) {
            //return chain.filter(exchange);
        //}
        String remoteIp = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();

        System.out.println("[GATEWAY] Request from IP: " + remoteIp + " → " + path);

        String token = exchange.getRequest().getHeaders().getFirst("X-Auth-Token");
        if (token == null || !tokenStore.isValid(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1;
    }
}