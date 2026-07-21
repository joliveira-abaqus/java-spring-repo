package br.com.alurafood.gateway.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthFilterTest {

    private static final String SECRET =
            "YWx1cmFmb29kLXNlY3JldC1rZXktand0LXNlY3VyaXR5LTI1Ni1iaXRzLW1pbmltdW0=";
    private static final String GATEWAY_SECRET = "test-gateway-secret";

    private AuthFilter authFilter;
    private AtomicReference<ServerWebExchange> exchangeCapturado;

    @BeforeEach
    void setUp() {
        authFilter = new AuthFilter();
        ReflectionTestUtils.setField(authFilter, "routeValidator", new RouteValidator());
        ReflectionTestUtils.setField(authFilter, "secret", SECRET);
        ReflectionTestUtils.setField(authFilter, "gatewaySecret", GATEWAY_SECRET);
        exchangeCapturado = new AtomicReference<>();
    }

    private final org.springframework.cloud.gateway.filter.GatewayFilterChain chain = exchange -> {
        exchangeCapturado.set(exchange);
        return Mono.empty();
    };

    private String gerarToken(String email, String role, long expiracaoMs) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiracaoMs))
                .signWith(key)
                .compact();
    }

    @Test
    void validateSecret_deveLancarExcecaoQuandoSecretVazio() {
        ReflectionTestUtils.setField(authFilter, "gatewaySecret", "  ");
        assertThrows(IllegalStateException.class, () -> authFilter.validateSecret());
    }

    @Test
    void filter_devePassarDiretoParaRotaPublica() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/auth/login"));

        authFilter.filter(exchange, chain).block();

        assertEquals(exchange, exchangeCapturado.get());
    }

    @Test
    void filter_deveRetornar401QuandoSemHeaderAuthorization() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/pagamentos"));

        authFilter.filter(exchange, chain).block();

        assertNull(exchangeCapturado.get());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void filter_deveRetornar401QuandoHeaderSemBearer() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/pagamentos")
                        .header(HttpHeaders.AUTHORIZATION, "Token abc"));

        authFilter.filter(exchange, chain).block();

        assertNull(exchangeCapturado.get());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void filter_deveRetornar401QuandoTokenInvalido() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/pagamentos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"));

        authFilter.filter(exchange, chain).block();

        assertNull(exchangeCapturado.get());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void filter_deveRetornar401QuandoTokenExpirado() {
        String token = gerarToken("admin@alurafood.com", "ROLE_USER", -1000);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/pagamentos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));

        authFilter.filter(exchange, chain).block();

        assertNull(exchangeCapturado.get());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void filter_devePropagarHeadersQuandoTokenValido() {
        String token = gerarToken("admin@alurafood.com", "ROLE_ADMIN", 60000);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/pagamentos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));

        authFilter.filter(exchange, chain).block();

        ServerWebExchange encaminhado = exchangeCapturado.get();
        assertEquals("admin@alurafood.com",
                encaminhado.getRequest().getHeaders().getFirst("X-Auth-User-Email"));
        assertEquals("ROLE_ADMIN",
                encaminhado.getRequest().getHeaders().getFirst("X-Auth-User-Role"));
        assertEquals(GATEWAY_SECRET,
                encaminhado.getRequest().getHeaders().getFirst("X-Gateway-Secret"));
    }

    @Test
    void getOrder_deveRetornarMenosUm() {
        assertEquals(-1, authFilter.getOrder());
    }

}
