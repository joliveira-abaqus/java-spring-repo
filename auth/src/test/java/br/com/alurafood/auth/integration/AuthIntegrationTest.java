package br.com.alurafood.auth.integration;

import br.com.alurafood.auth.dto.LoginRequest;
import br.com.alurafood.auth.dto.LoginResponse;
import br.com.alurafood.auth.dto.RegistroRequest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private static String tokenValido;

    @Test
    @Order(1)
    void deveRegistrarUsuarioComSucesso() {
        RegistroRequest request = new RegistroRequest("Admin", "admin@alurafood.com", "123456");

        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/auth/registro", request, LoginResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getToken());
        assertEquals("Admin", response.getBody().getNome());
        assertEquals("admin@alurafood.com", response.getBody().getEmail());
        assertEquals("ROLE_USER", response.getBody().getRole());

        tokenValido = response.getBody().getToken();
    }

    @Test
    @Order(2)
    void deveRealizarLoginComSucesso() {
        LoginRequest request = new LoginRequest("admin@alurafood.com", "123456");

        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/auth/login", request, LoginResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getToken());
        assertEquals("Admin", response.getBody().getNome());
        assertEquals("admin@alurafood.com", response.getBody().getEmail());

        tokenValido = response.getBody().getToken();
    }

    @Test
    @Order(3)
    void deveValidarTokenComSucesso() {
        assertNotNull(tokenValido, "Token deve ter sido gerado nos testes anteriores");

        ResponseEntity<Void> response = restTemplate.getForEntity(
                "/auth/validar?token=" + tokenValido, Void.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @Order(4)
    void deveRetornar401ParaTokenInvalido() {
        ResponseEntity<Void> response = restTemplate.getForEntity(
                "/auth/validar?token=token_invalido_qualquer", Void.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @Order(5)
    void deveRejeitarRegistroComEmailDuplicado() {
        RegistroRequest request = new RegistroRequest("Admin2", "admin@alurafood.com", "654321");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/auth/registro", request, String.class);

        assertNotEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @Order(6)
    void deveRejeitarLoginComSenhaErrada() {
        LoginRequest request = new LoginRequest("admin@alurafood.com", "senhaerrada");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/auth/login", request, String.class);

        assertTrue(
                response.getStatusCode() == HttpStatus.UNAUTHORIZED
                        || response.getStatusCode() == HttpStatus.FORBIDDEN
                        || response.getStatusCode().is4xxClientError()
        );
    }

    @Test
    @Order(7)
    void deveRejeitarLoginComEmailInexistente() {
        LoginRequest request = new LoginRequest("naoexiste@alurafood.com", "123456");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/auth/login", request, String.class);

        assertTrue(response.getStatusCode().is4xxClientError());
    }
}
