package br.com.alurafood.auth.config;

import br.com.alurafood.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_deveSeguirSemAutenticarQuandoNaoHaHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService);
    }

    @Test
    void doFilterInternal_deveSeguirSemAutenticarQuandoHeaderNaoEBearer() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Token abc");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_deveSeguirSemAutenticarQuandoTokenLancaExcecao() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-invalido");
        when(jwtService.extrairEmail("token-invalido")).thenThrow(new RuntimeException("invalido"));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_deveAutenticarQuandoTokenValido() throws Exception {
        UserDetails userDetails = User.withUsername("admin@alurafood.com")
                .password("x").roles("USER").build();
        when(request.getHeader("Authorization")).thenReturn("Bearer token-valido");
        when(jwtService.extrairEmail("token-valido")).thenReturn("admin@alurafood.com");
        when(userDetailsService.loadUserByUsername("admin@alurafood.com")).thenReturn(userDetails);
        when(jwtService.isTokenValido("token-valido", userDetails)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals("admin@alurafood.com",
                SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void doFilterInternal_naoDeveAutenticarQuandoTokenNaoEValido() throws Exception {
        UserDetails userDetails = User.withUsername("admin@alurafood.com")
                .password("x").roles("USER").build();
        when(request.getHeader("Authorization")).thenReturn("Bearer token-valido");
        when(jwtService.extrairEmail("token-valido")).thenReturn("admin@alurafood.com");
        when(userDetailsService.loadUserByUsername("admin@alurafood.com")).thenReturn(userDetails);
        when(jwtService.isTokenValido("token-valido", userDetails)).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
