package br.com.alurafood.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteValidatorTest {

    private final RouteValidator routeValidator = new RouteValidator();

    private ServerHttpRequest requisicaoPara(String caminho) {
        return MockServerHttpRequest.get(caminho).build();
    }

    @Test
    void isRotaProtegida_deveConsiderarRegistroComoPublica() {
        assertFalse(routeValidator.isRotaProtegida.test(requisicaoPara("/auth/registro")));
    }

    @Test
    void isRotaProtegida_deveConsiderarLoginComoPublica() {
        assertFalse(routeValidator.isRotaProtegida.test(requisicaoPara("/auth/login")));
    }

    @Test
    void isRotaProtegida_deveConsiderarValidarComoPublica() {
        assertFalse(routeValidator.isRotaProtegida.test(requisicaoPara("/auth/validar")));
    }

    @Test
    void isRotaProtegida_deveConsiderarEurekaComoPublica() {
        assertFalse(routeValidator.isRotaProtegida.test(requisicaoPara("/eureka/apps")));
    }

    @Test
    void isRotaProtegida_deveConsiderarRotaPublicaComPrefixoDoServico() {
        assertFalse(routeValidator.isRotaProtegida.test(requisicaoPara("/auth-service/auth/login")));
    }

    @Test
    void isRotaProtegida_deveConsiderarPagamentosComoProtegida() {
        assertTrue(routeValidator.isRotaProtegida.test(requisicaoPara("/pagamentos")));
    }

    @Test
    void isRotaProtegida_deveConsiderarPedidosComoProtegida() {
        assertTrue(routeValidator.isRotaProtegida.test(requisicaoPara("/pedidos/1")));
    }
}
