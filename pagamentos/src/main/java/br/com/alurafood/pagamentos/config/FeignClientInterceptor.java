package br.com.alurafood.pagamentos.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class FeignClientInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String authorization = request.getHeader("Authorization");
            if (authorization != null) {
                template.header("Authorization", authorization);
            }
            String userEmail = request.getHeader("X-Auth-User-Email");
            if (userEmail != null) {
                template.header("X-Auth-User-Email", userEmail);
            }
            String userRole = request.getHeader("X-Auth-User-Role");
            if (userRole != null) {
                template.header("X-Auth-User-Role", userRole);
            }
            String gatewaySecret = request.getHeader("X-Gateway-Secret");
            if (gatewaySecret != null) {
                template.header("X-Gateway-Secret", gatewaySecret);
            }
        }
    }
}
