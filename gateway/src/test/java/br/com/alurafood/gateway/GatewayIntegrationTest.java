package br.com.alurafood.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private String tokenValido;

    @BeforeEach
    void setUp() {
        tokenValido = gerarToken("admin@alurafood.com", "ROLE_USER", 3600000);
    }

    @Test
    void deveRetornar401ParaRotaProtegidaSemToken() {
        webTestClient.get()
                .uri("/pagamentos")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void deveRetornar401ParaRotaProtegidaComTokenInvalido() {
        webTestClient.get()
                .uri("/pagamentos")
                .header("Authorization", "Bearer token_invalido")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void deveRetornar401ParaRotaProtegidaComTokenExpirado() {
        String tokenExpirado = gerarToken("user@test.com", "ROLE_USER", -3600000);

        webTestClient.get()
                .uri("/pagamentos")
                .header("Authorization", "Bearer " + tokenExpirado)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void deveRetornar401ParaHeaderAuthorizationSemBearer() {
        webTestClient.get()
                .uri("/pagamentos")
                .header("Authorization", "Basic abc123")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void devePermitirAcessoRotaDeLoginSemToken() {
        webTestClient.post()
                .uri("/auth/login")
                .exchange()
                .expectStatus().value(status -> assertNotEquals(HttpStatus.UNAUTHORIZED.value(), status));
    }

    @Test
    void devePermitirAcessoRotaDeRegistroSemToken() {
        webTestClient.post()
                .uri("/auth/registro")
                .exchange()
                .expectStatus().value(status -> assertNotEquals(HttpStatus.UNAUTHORIZED.value(), status));
    }

    @Test
    void devePermitirAcessoRotaDeValidarSemToken() {
        webTestClient.get()
                .uri("/auth/validar")
                .exchange()
                .expectStatus().value(status -> assertNotEquals(HttpStatus.UNAUTHORIZED.value(), status));
    }

    @Test
    void devePermitirAcessoRotaEurekaSemToken() {
        webTestClient.get()
                .uri("/eureka")
                .exchange()
                .expectStatus().value(status -> assertNotEquals(HttpStatus.UNAUTHORIZED.value(), status));
    }

    @Test
    void deveAceitarTokenValidoParaRotaProtegida() {
        // Com token válido, o gateway deve aceitar e tentar rotear.
        // Como não há serviço downstream no teste, esperamos 503 (Service Unavailable)
        // ao invés de 401, confirmando que o filtro aceitou o token.
        webTestClient.get()
                .uri("/pagamentos")
                .header("Authorization", "Bearer " + tokenValido)
                .exchange()
                .expectStatus().value(status -> assertNotEquals(HttpStatus.UNAUTHORIZED.value(), status));
    }

    @Test
    void deveAceitarTokenComRoleAdmin() {
        String tokenAdmin = gerarToken("admin@test.com", "ROLE_ADMIN", 3600000);

        webTestClient.get()
                .uri("/pedidos")
                .header("Authorization", "Bearer " + tokenAdmin)
                .exchange()
                .expectStatus().value(status -> assertNotEquals(HttpStatus.UNAUTHORIZED.value(), status));
    }

    @Test
    void deveRetornar401QuandoAuthorizationHeaderVazio() {
        webTestClient.get()
                .uri("/pagamentos")
                .header("Authorization", "")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void deveRetornar401QuandoBearerSemToken() {
        webTestClient.get()
                .uri("/pagamentos")
                .header("Authorization", "Bearer ")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private String gerarToken(String subject, String role, long validadeMs) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
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
