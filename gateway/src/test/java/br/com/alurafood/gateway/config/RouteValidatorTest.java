package br.com.alurafood.gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.assertj.core.api.Assertions.assertThat;

class RouteValidatorTest {

    private RouteValidator routeValidator;

    @BeforeEach
    void setUp() {
        routeValidator = new RouteValidator();
    }

    @Nested
    @DisplayName("Rotas publicas")
    class RotasPublicas {

        @ParameterizedTest
        @ValueSource(strings = {
                "/auth/login",
                "/auth/registro",
                "/auth/validar",
                "/eureka",
                "/eureka/apps",
                "/eureka/apps/gateway"
        })
        @DisplayName("deve identificar rotas publicas sem prefixo de servico")
        void deveIdentificarRotasPublicasSemPrefixo(String path) {
            var request = MockServerHttpRequest.get(path).build();
            assertThat(routeValidator.isRotaProtegida.test(request)).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "/auth-service/auth/login",
                "/auth-service/auth/registro",
                "/auth-service/auth/validar",
                "/server/eureka",
                "/server/eureka/apps"
        })
        @DisplayName("deve identificar rotas publicas com prefixo de servico")
        void deveIdentificarRotasPublicasComPrefixo(String path) {
            var request = MockServerHttpRequest.get(path).build();
            assertThat(routeValidator.isRotaProtegida.test(request)).isFalse();
        }
    }

    @Nested
    @DisplayName("Rotas protegidas")
    class RotasProtegidas {

        @ParameterizedTest
        @ValueSource(strings = {
                "/pedidos",
                "/pedidos/1",
                "/pagamentos",
                "/pagamentos/1",
                "/pagamentos/1/confirmar",
                "/api/qualquer-rota",
                "/auth/outra-rota"
        })
        @DisplayName("deve identificar rotas protegidas")
        void deveIdentificarRotasProtegidas(String path) {
            var request = MockServerHttpRequest.get(path).build();
            assertThat(routeValidator.isRotaProtegida.test(request)).isTrue();
        }
    }

    @Nested
    @DisplayName("Casos limite")
    class CasosLimite {

        @Test
        @DisplayName("rota raiz deve ser protegida")
        void rotaRaizDeveSerProtegida() {
            var request = MockServerHttpRequest.get("/").build();
            assertThat(routeValidator.isRotaProtegida.test(request)).isTrue();
        }

        @Test
        @DisplayName("rota vazia deve ser protegida")
        void rotaVaziaDeveSerProtegida() {
            var request = MockServerHttpRequest.get("").build();
            assertThat(routeValidator.isRotaProtegida.test(request)).isTrue();
        }

        @Test
        @DisplayName("login com query params nao deve afetar validacao")
        void loginComQueryParamsNaoDeveAfetarValidacao() {
            var request = MockServerHttpRequest.get("/auth/login?redirect=/dashboard").build();
            assertThat(routeValidator.isRotaProtegida.test(request)).isFalse();
        }
    }
}
