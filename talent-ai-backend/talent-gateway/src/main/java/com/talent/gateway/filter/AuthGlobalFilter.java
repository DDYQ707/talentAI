package com.talent.gateway.filter;

import com.talent.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 网关全局鉴权过滤器
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    // 1. 设置白名单：不需要携带 Token 就能直接访问的接口
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/auth/login",
            "/api/auth/login/otp",
            "/api/auth/otp/send",
            "/api/auth/register",
            "/internal/ai/**",
            "/internal/interview/**",
            "/api/interview/internal/**"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 2. 判断请求路径是否在白名单中
        for (String pattern : WHITE_LIST) {
            if (pathMatcher.match(pattern, path)) {
                return chain.filter(exchange); // 白名单接口，直接放行
            }
        }

        // 3. 不是白名单接口，从请求头中获取 Token
        // 行业规范：Token 通常放在 Header 的 Authorization 字段，并以 "Bearer " 开头
        String authHeader = request.getHeaders().getFirst("Authorization");
        String token = null;
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7); // 截取掉 "Bearer " 这7个字符，剩下的就是纯 Token
        }

        // 4. 如果没有 Token，直接拦截，返回 401 未授权
        if (!StringUtils.hasText(token)) {
            return unauthorizedResponse(exchange);
        }

        // 5. 校验 Token 的合法性
        try {
            Claims claims = JwtUtil.parseToken(token);

            // 校验通过后，将 JWT 中的用户信息写入请求头，供下游服务直接使用（无需再次解析 Token）
            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role != null ? role : "")
                    .build();
            ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

            return chain.filter(mutatedExchange);

        } catch (Exception e) {
            // 解析失败（Token 过期、被篡改等），返回 401
            return unauthorizedResponse(exchange);
        }
    }

    /**
     * 统一封装 401 未授权响应
     */
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = "{\"code\":401,\"msg\":\"未授权，请先登录\"}".getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * 过滤器的执行顺序，数字越小优先级越高。
     * -1 表示在路由转发前最先执行
     */
    @Override
    public int getOrder() {
        return -1;
    }
}