package br.com.alurafood.pagamentos.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GatewayAuthFilterTest {

    private static final String GATEWAY_SECRET = "test-gateway-secret";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private GatewayAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new GatewayAuthFilter();
        ReflectionTestUtils.setField(filter, "gatewaySecret", GATEWAY_SECRET);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validateSecret_deveLancarExcecaoQuandoSecretVazio() {
        ReflectionTestUtils.setField(filter, "gatewaySecret", "");
        assertThrows(IllegalStateException.class, () -> filter.validateSecret());
    }

    @Test
    void doFilterInternal_devePassarParaActuatorSemValidar() throws Exception {
        when(request.getRequestURI()).thenReturn("/actuator/health");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(response);
    }

    @Test
    void doFilterInternal_deveRetornar401QuandoSemSecret() throws Exception {
        when(request.getRequestURI()).thenReturn("/pagamentos");
        when(request.getHeader("X-Gateway-Secret")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
        verifyNoInteractions(filterChain);
    }

    @Test
    void doFilterInternal_deveRetornar401QuandoSecretInvalido() throws Exception {
        when(request.getRequestURI()).thenReturn("/pagamentos");
        when(request.getHeader("X-Gateway-Secret")).thenReturn("secret-errado");

        filter.doFilter(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
        verifyNoInteractions(filterChain);
    }

    @Test
    void doFilterInternal_deveAutenticarComRoleValida() throws Exception {
        when(request.getRequestURI()).thenReturn("/pagamentos");
        when(request.getHeader("X-Gateway-Secret")).thenReturn(GATEWAY_SECRET);
        when(request.getHeader("X-Auth-User-Email")).thenReturn("admin@alurafood.com");
        when(request.getHeader("X-Auth-User-Role")).thenReturn("ROLE_ADMIN");

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("admin@alurafood.com", auth.getName());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_deveAutenticarSemAuthoritiesQuandoRoleInvalida() throws Exception {
        when(request.getRequestURI()).thenReturn("/pagamentos");
        when(request.getHeader("X-Gateway-Secret")).thenReturn(GATEWAY_SECRET);
        when(request.getHeader("X-Auth-User-Email")).thenReturn("admin@alurafood.com");
        when(request.getHeader("X-Auth-User-Role")).thenReturn("ROLE_HACKER");

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("admin@alurafood.com", auth.getName());
        assertTrue(auth.getAuthorities().isEmpty());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_naoDeveAutenticarQuandoSemEmail() throws Exception {
        when(request.getRequestURI()).thenReturn("/pagamentos");
        when(request.getHeader("X-Gateway-Secret")).thenReturn(GATEWAY_SECRET);
        when(request.getHeader("X-Auth-User-Email")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
