package br.com.alurafood.gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.junit.jupiter.api.Assertions.*;

class RouteValidatorTest {

    private RouteValidator routeValidator;

    @BeforeEach
    void setUp() {
        routeValidator = new RouteValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/auth/login",
            "/auth/registro",
            "/auth/validar",
            "/eureka",
            "/eureka/apps",
            "/ms-auth/auth/login",
            "/ms-auth/auth/registro",
            "/ms-auth/auth/validar",
            "/servico/eureka",
            "/servico/eureka/apps/gateway"
    })
    void deveIdentificarRotasPublicas(String path) {
        MockServerHttpRequest request = MockServerHttpRequest.get(path).build();

        boolean resultado = routeValidator.isRotaProtegida.test(request);

        assertFalse(resultado, "Rota deveria ser pública: " + path);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/pagamentos",
            "/pagamentos/1",
            "/pedidos",
            "/pedidos/1",
            "/pedidos/1/status",
            "/api/usuarios",
            "/ms-pagamentos/pagamentos",
            "/ms-pedidos/pedidos/1/confirmar"
    })
    void deveIdentificarRotasProtegidas(String path) {
        MockServerHttpRequest request = MockServerHttpRequest.get(path).build();

        boolean resultado = routeValidator.isRotaProtegida.test(request);

        assertTrue(resultado, "Rota deveria ser protegida: " + path);
    }

    @Test
    void deveProtegerRotaRaiz() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/").build();

        boolean resultado = routeValidator.isRotaProtegida.test(request);

        assertTrue(resultado);
    }

    @Test
    void deveProtegerRotaComPathSimilarMasNaoExato() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/auth/outros").build();

        boolean resultado = routeValidator.isRotaProtegida.test(request);

        assertTrue(resultado);
    }

    @Test
    void devePermitirLoginComSubpath() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/auth/login/oauth").build();

        boolean resultado = routeValidator.isRotaProtegida.test(request);

        assertFalse(resultado);
    }

    @Test
    void devePermitirRegistroComSubpath() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/auth/registro/confirmar").build();

        boolean resultado = routeValidator.isRotaProtegida.test(request);

        assertFalse(resultado);
    }

    @Test
    void devePermitirValidarComSubpath() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/auth/validar/token").build();

        boolean resultado = routeValidator.isRotaProtegida.test(request);

        assertFalse(resultado);
    }

    @Test
    void deveProtegerRotaAuthSemSubcaminho() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/auth").build();

        boolean resultado = routeValidator.isRotaProtegida.test(request);

        assertTrue(resultado);
    }
}
