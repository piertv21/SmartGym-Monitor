package com.smartgym.gateway.config;

import com.smartgym.gateway.service.JwtValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
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

    private final JwtValidationService jwtValidationService;

    public ExternalAuthFilter(JwtValidationService jwtValidationService) {
        this.jwtValidationService = jwtValidationService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getPath();
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String routeId = route != null ? route.getId() : "unknown";
        String routeUri = route != null ? String.valueOf(route.getUri()) : "unknown";

        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        String remoteIp = remoteAddress != null && remoteAddress.getAddress() != null
                ? remoteAddress.getAddress().getHostAddress()
                : "unknown";

        log.info("[GATEWAY] IN {} {} from {} -> route={} uri={}", method, path, remoteIp, routeId, routeUri);

        // Public endpoints that must remain accessible without a JWT.
        if (path.startsWith("/actuator")
                || path.endsWith("/login")
                || path.endsWith("/register")
                || path.endsWith("/logout")) {
            return forwardWithLogging(exchange, chain, method, path, routeId);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            log.warn("[GATEWAY] BLOCKED {} {} from {} -> route={} reason=missing_bearer_token", method, path, remoteIp, routeId);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        try {
            String userId = jwtValidationService.validateAndExtractUserId(token);
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .build();
            exchange = exchange.mutate().request(mutatedRequest).build();
        } catch (IllegalStateException ex) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            log.warn("[GATEWAY] BLOCKED {} {} from {} -> route={} reason=invalid_jwt", method, path, remoteIp, routeId);
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