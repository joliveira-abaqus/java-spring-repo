package br.com.alurafood.auth.service;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET =
            "YWx1cmFmb29kLXNlY3JldC1rZXktand0LXNlY3VyaXR5LTI1Ni1iaXRzLW1pbmltdW0=";

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);
        userDetails = User.withUsername("admin@alurafood.com").password("x").roles("USER").build();
    }

    @Test
    void gerarToken_deveGerarTokenComTresPartes() {
        String token = jwtService.gerarToken(userDetails);

        assertNotNull(token);
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    void extrairEmail_deveRetornarSubjectDoToken() {
        String token = jwtService.gerarToken(userDetails);

        assertEquals("admin@alurafood.com", jwtService.extrairEmail(token));
    }

    @Test
    void extrairClaim_deveRetornarClaimExtra() {
        String token = jwtService.gerarToken(Map.of("role", "ROLE_ADMIN"), userDetails);

        String role = jwtService.extrairClaim(token, claims -> claims.get("role", String.class));

        assertEquals("ROLE_ADMIN", role);
    }

    @Test
    void isTokenValido_comUserDetails_deveRetornarTrueParaTokenValido() {
        String token = jwtService.gerarToken(userDetails);

        assertTrue(jwtService.isTokenValido(token, userDetails));
    }

    @Test
    void isTokenValido_comUserDetails_deveRetornarFalseParaOutroUsuario() {
        String token = jwtService.gerarToken(userDetails);
        UserDetails outro = User.withUsername("outro@alurafood.com").password("x").roles("USER").build();

        assertFalse(jwtService.isTokenValido(token, outro));
    }

    @Test
    void isTokenValido_comUserDetails_deveLancarExcecaoParaTokenExpirado() {
        ReflectionTestUtils.setField(jwtService, "expiration", -1000L);
        String token = jwtService.gerarToken(userDetails);
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);

        assertThrows(ExpiredJwtException.class, () -> jwtService.isTokenValido(token, userDetails));
    }

    @Test
    void isTokenValido_deveRetornarFalseParaTokenExpirado() {
        ReflectionTestUtils.setField(jwtService, "expiration", -1000L);
        String token = jwtService.gerarToken(userDetails);
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);

        assertFalse(jwtService.isTokenValido(token));
    }

    @Test
    void isTokenValido_deveRetornarTrueParaTokenBemFormado() {
        String token = jwtService.gerarToken(userDetails);

        assertTrue(jwtService.isTokenValido(token));
    }

    @Test
    void isTokenValido_deveRetornarFalseParaTokenInvalido() {
        assertFalse(jwtService.isTokenValido("token-invalido"));
    }
}
