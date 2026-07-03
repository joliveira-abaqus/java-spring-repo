package br.com.alurafood.gateway.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthFilterTest {

    private static final String JWT_SECRET = "YWx1cmFmb29kLXNlY3JldC1rZXktand0LXNlY3VyaXR5LTI1Ni1iaXRzLW1pbmltdW0=";
    private static final String GATEWAY_SECRET = "test-gateway-secret";

    @InjectMocks
    private AuthFilter authFilter;

    @Mock
    private RouteValidator routeValidator;

    @Mock
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authFilter, "secret", JWT_SECRET);
        ReflectionTestUtils.setField(authFilter, "gatewaySecret", GATEWAY_SECRET);
    }

    @Test
    void devePermitirRotaPublicaSemAutenticacao() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        routeValidator.isRotaProtegida = req -> false;
        when(chain.filter(any())).thenReturn(Mono.empty());

        Mono<Void> resultado = authFilter.filter(exchange, chain);

        StepVerifier.create(resultado).verifyComplete();
        verify(chain).filter(any());
    }

    @Test
    void deveRetornar401QuandoSemHeaderAuthorization() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/pagamentos").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        routeValidator.isRotaProtegida = req -> true;

        Mono<Void> resultado = authFilter.filter(exchange, chain);

        StepVerifier.create(resultado).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void deveRetornar401QuandoHeaderSemPrefixoBearer() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/pagamentos")
                .header(HttpHeaders.AUTHORIZATION, "Basic abc123")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        routeValidator.isRotaProtegida = req -> true;

        Mono<Void> resultado = authFilter.filter(exchange, chain);

        StepVerifier.create(resultado).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void deveRetornar401QuandoTokenInvalido() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/pagamentos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token_invalido")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        routeValidator.isRotaProtegida = req -> true;

        Mono<Void> resultado = authFilter.filter(exchange, chain);

        StepVerifier.create(resultado).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void deveRetornar401QuandoTokenExpirado() {
        String tokenExpirado = gerarToken("user@test.com", "ROLE_USER", -3600000);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/pagamentos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenExpirado)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        routeValidator.isRotaProtegida = req -> true;

        Mono<Void> resultado = authFilter.filter(exchange, chain);

        StepVerifier.create(resultado).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    void devePropagarHeadersQuandoTokenValido() {
        String tokenValido = gerarToken("admin@alurafood.com", "ROLE_USER", 3600000);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/pagamentos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenValido)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        routeValidator.isRotaProtegida = req -> true;
        when(chain.filter(any())).thenReturn(Mono.empty());

        Mono<Void> resultado = authFilter.filter(exchange, chain);

        StepVerifier.create(resultado).verifyComplete();
        verify(chain).filter(argThat(ex -> {
            var headers = ex.getRequest().getHeaders();
            return "admin@alurafood.com".equals(headers.getFirst("X-Auth-User-Email"))
                    && "ROLE_USER".equals(headers.getFirst("X-Auth-User-Role"))
                    && GATEWAY_SECRET.equals(headers.getFirst("X-Gateway-Secret"));
        }));
    }

    @Test
    void deveExtrairEmailDoSubjectDoToken() {
        String email = "teste@dominio.com";
        String tokenValido = gerarToken(email, "ROLE_ADMIN", 3600000);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/pedidos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenValido)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        routeValidator.isRotaProtegida = req -> true;
        when(chain.filter(any())).thenReturn(Mono.empty());

        authFilter.filter(exchange, chain);

        verify(chain).filter(argThat(ex ->
                email.equals(ex.getRequest().getHeaders().getFirst("X-Auth-User-Email"))
        ));
    }

    @Test
    void deveExtrairRoleDoToken() {
        String tokenValido = gerarToken("user@test.com", "ROLE_ADMIN", 3600000);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/pedidos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenValido)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        routeValidator.isRotaProtegida = req -> true;
        when(chain.filter(any())).thenReturn(Mono.empty());

        authFilter.filter(exchange, chain);

        verify(chain).filter(argThat(ex ->
                "ROLE_ADMIN".equals(ex.getRequest().getHeaders().getFirst("X-Auth-User-Role"))
        ));
    }

    @Test
    void deveAdicionarGatewaySecretNoHeader() {
        String tokenValido = gerarToken("user@test.com", "ROLE_USER", 3600000);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/pedidos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenValido)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        routeValidator.isRotaProtegida = req -> true;
        when(chain.filter(any())).thenReturn(Mono.empty());

        authFilter.filter(exchange, chain);

        verify(chain).filter(argThat(ex ->
                GATEWAY_SECRET.equals(ex.getRequest().getHeaders().getFirst("X-Gateway-Secret"))
        ));
    }

    @Test
    void deveRetornarOrdemMenosUm() {
        assertEquals(-1, authFilter.getOrder());
    }

    @Test
    void validateSecret_deveLancarExcecaoQuandoGatewaySecretVazio() {
        ReflectionTestUtils.setField(authFilter, "gatewaySecret", "");

        assertThrows(IllegalStateException.class, () -> authFilter.validateSecret());
    }

    @Test
    void validateSecret_deveLancarExcecaoQuandoGatewaySecretNulo() {
        ReflectionTestUtils.setField(authFilter, "gatewaySecret", null);

        assertThrows(IllegalStateException.class, () -> authFilter.validateSecret());
    }

    @Test
    void validateSecret_devePassarQuandoGatewaySecretConfigurado() {
        ReflectionTestUtils.setField(authFilter, "gatewaySecret", "meu-secret");

        assertDoesNotThrow(() -> authFilter.validateSecret());
    }

    private String gerarToken(String subject, String role, long validadeMs) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET));
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + validadeMs);

        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(key)
                .compact();
    }
}
