package br.com.alurafood.pagamentos.config;

import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeignClientInterceptorTest {

    private final FeignClientInterceptor interceptor = new FeignClientInterceptor();

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void apply_deveEncaminharHeadersPresentes() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        request.addHeader("X-Auth-User-Email", "admin@alurafood.com");
        request.addHeader("X-Auth-User-Role", "ROLE_USER");
        request.addHeader("X-Gateway-Secret", "test-gateway-secret");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertEquals("Bearer token", template.headers().get("Authorization").iterator().next());
        assertEquals("admin@alurafood.com", template.headers().get("X-Auth-User-Email").iterator().next());
        assertEquals("ROLE_USER", template.headers().get("X-Auth-User-Role").iterator().next());
        assertEquals("test-gateway-secret", template.headers().get("X-Gateway-Secret").iterator().next());
    }

    @Test
    void apply_naoDeveEncaminharHeadersAusentes() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertTrue(template.headers().containsKey("Authorization"));
        assertTrue(template.headers().get("X-Auth-User-Email") == null
                || template.headers().get("X-Auth-User-Email").isEmpty());
    }

    @Test
    void apply_naoDeveFalharSemRequestAttributes() {
        RequestContextHolder.resetRequestAttributes();

        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertTrue(template.headers().isEmpty());
    }
}
