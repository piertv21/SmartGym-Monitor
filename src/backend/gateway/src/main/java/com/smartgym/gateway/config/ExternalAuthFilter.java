package com.smartgym.gateway.config;

import com.smartgym.gateway.service.TokenStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import java.net.InetSocketAddress;
import java.net.URI;
import reactor.core.publisher.Mono;

@Component
public class ExternalAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(ExternalAuthFilter.class);

    private final TokenStore tokenStore;

    public ExternalAuthFilter(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String method = exchange.getRequest().getMethod() != null
                ? exchange.getRequest().getMethod().name()
                : "UNKNOWN";
        String path = exchange.getRequest().getURI().getPath();
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String routeId = route != null ? route.getId() : "unknown";
        String routeUri = route != null ? String.valueOf(route.getUri()) : "unknown";

        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        String remoteIp = remoteAddress != null && remoteAddress.getAddress() != null
                ? remoteAddress.getAddress().getHostAddress()
                : "unknown";

        log.info("[GATEWAY] IN {} {} from {} -> route={} uri={}", method, path, remoteIp, routeId, routeUri);

        // Keep token generation and actuator reachable without pre-existing gateway token.
        if (path.startsWith("/auth/generate")
                || path.startsWith("/actuator")) {
            return forwardWithLogging(exchange, chain, method, path, routeId);
        }

        String token = exchange.getRequest().getHeaders().getFirst("X-Auth-Token");
        if (token == null || !tokenStore.isValid(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            log.warn("[GATEWAY] BLOCKED {} {} from {} -> route={} reason=missing_or_invalid_token", method, path, remoteIp, routeId);
            return exchange.getResponse().setComplete();
        }

        return forwardWithLogging(exchange, chain, method, path, routeId);
    }

    private Mono<Void> forwardWithLogging(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            String method,
            String path,
            String routeId
    ) {
        return chain.filter(exchange)
                .doFinally(signalType -> {
                    URI forwardedUri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
                    String status = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().toString()
                            : "unknown";
                    log.info(
                            "[GATEWAY] OUT {} {} -> route={} forwardedTo={} status={}",
                            method,
                            path,
                            routeId,
                            forwardedUri != null ? forwardedUri : "unknown",
                            status
                    );
                });
    }

    @Override
    public int getOrder() {
        return -1;
    }
}