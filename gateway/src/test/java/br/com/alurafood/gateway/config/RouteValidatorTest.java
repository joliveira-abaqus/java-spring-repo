package br.com.alurafood.gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

    @ParameterizedTest
    @ValueSource(strings = {
            "/auth/registro",
            "/auth/login",
            "/auth/validar",
            "/eureka",
            "/eureka/apps",
            "/ms-auth/auth/registro",
            "/ms-auth/auth/login/extra"
    })
    @DisplayName("Deve identificar rotas públicas como NÃO protegidas")
    void deveRetornarFalseParaRotasPublicas(String path) {
        var request = MockServerHttpRequest.get(path).build();

        boolean protegida = routeValidator.isRotaProtegida.test(request);

        assertThat(protegida).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/pagamentos",
            "/pedidos",
            "/pedidos/1",
            "/pagamentos/confirmar"
    })
    @DisplayName("Deve identificar rotas de negócio como protegidas")
    void deveRetornarTrueParaRotasProtegidas(String path) {
        var request = MockServerHttpRequest.get(path).build();

        boolean protegida = routeValidator.isRotaProtegida.test(request);

        assertThat(protegida).isTrue();
    }
}
