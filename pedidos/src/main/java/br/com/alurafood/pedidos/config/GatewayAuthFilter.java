package br.com.alurafood.pedidos.config;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class GatewayAuthFilter extends OncePerRequestFilter {

    @Value("${gateway.secret}")
    private String gatewaySecret;

    private static final List<String> ALLOWED_ROLES = List.of("ROLE_USER", "ROLE_ADMIN");

    @PostConstruct
    void validateSecret() {
        if (gatewaySecret == null || gatewaySecret.isBlank()) {
            throw new IllegalStateException("gateway.secret não configurado — defina GATEWAY_SECRET como variável de ambiente");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (isActuatorRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String receivedSecret = request.getHeader("X-Gateway-Secret");

        if (receivedSecret == null || !constantTimeEquals(receivedSecret, gatewaySecret)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }

        String userEmail = request.getHeader("X-Auth-User-Email");
        String userRole = request.getHeader("X-Auth-User-Role");

        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            List<SimpleGrantedAuthority> authorities = (userRole != null && ALLOWED_ROLES.contains(userRole))
                    ? List.of(new SimpleGrantedAuthority(userRole))
                    : List.of();

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userEmail, null, authorities
            );
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isActuatorRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.startsWith("/actuator");
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(aBytes, bBytes);
    }
}
