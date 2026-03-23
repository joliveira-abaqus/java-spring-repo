package br.com.alurafood.pedidos.config;

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
import java.util.List;

@Component
public class GatewayAuthFilter extends OncePerRequestFilter {

    @Value("${gateway.secret}")
    private String gatewaySecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String receivedSecret = request.getHeader("X-Gateway-Secret");

        if (receivedSecret == null || !receivedSecret.equals(gatewaySecret)) {
            filterChain.doFilter(request, response);
            return;
        }

        String userEmail = request.getHeader("X-Auth-User-Email");
        String userRole = request.getHeader("X-Auth-User-Role");

        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            List<SimpleGrantedAuthority> authorities = userRole != null
                    ? List.of(new SimpleGrantedAuthority(userRole))
                    : List.of();

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userEmail, null, authorities
            );
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}
