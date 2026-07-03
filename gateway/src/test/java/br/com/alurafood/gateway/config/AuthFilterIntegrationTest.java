package br.com.alurafood.gateway.config;

import br.com.alurafood.gateway.util.JwtTestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
class AuthFilterIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Nested
    @DisplayName("Fluxo de autenticacao completo")
    class FluxoAutenticacao {

        @Test
        @DisplayName("deve rejeitar requisicao protegida sem token")
        void deveRejeitar401SemToken() {
            webTestClient.get()
                    .uri("/pedidos")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("deve rejeitar requisicao com token expirado")
        void deveRejeitar401ComTokenExpirado() {
            String tokenExpirado = JwtTestUtil.gerarTokenExpirado("user@test.com", "USER");
            webTestClient.get()
                    .uri("/pedidos")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenExpirado)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("deve rejeitar requisicao com assinatura invalida")
        void deveRejeitar401ComAssinaturaInvalida() {
            String tokenInvalido = JwtTestUtil.gerarTokenComChaveInvalida("user@test.com", "USER");
            webTestClient.get()
                    .uri("/pagamentos")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenInvalido)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("deve rejeitar requisicao com token malformado")
        void deveRejeitar401ComTokenMalformado() {
            webTestClient.get()
                    .uri("/pedidos")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer nao-e-jwt")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("deve rejeitar requisicao sem prefixo Bearer")
        void deveRejeitar401SemPrefixoBearer() {
            String token = JwtTestUtil.gerarToken("user@test.com", "USER");
            webTestClient.get()
                    .uri("/pedidos")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + token)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }
    }

    @Nested
    @DisplayName("Rotas publicas")
    class RotasPublicas {

        @Test
        @DisplayName("rota /auth/login nao exige token")
        void rotaLoginNaoExigeToken() {
            webTestClient.get()
                    .uri("/auth/login")
                    .exchange()
                    .expectStatus().value(status ->
                            org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(401));
        }

        @Test
        @DisplayName("rota /auth/registro nao exige token")
        void rotaRegistroNaoExigeToken() {
            webTestClient.get()
                    .uri("/auth/registro")
                    .exchange()
                    .expectStatus().value(status ->
                            org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(401));
        }

        @Test
        @DisplayName("rota /auth/validar nao exige token")
        void rotaValidarNaoExigeToken() {
            webTestClient.get()
                    .uri("/auth/validar")
                    .exchange()
                    .expectStatus().value(status ->
                            org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(401));
        }
    }
}
