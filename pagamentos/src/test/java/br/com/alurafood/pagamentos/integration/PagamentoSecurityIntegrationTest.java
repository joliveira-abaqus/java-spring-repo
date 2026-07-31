package br.com.alurafood.pagamentos.integration;

import br.com.alurafood.pagamentos.dto.PagamentoDto;
import br.com.alurafood.pagamentos.http.PedidoClient;
import br.com.alurafood.pagamentos.model.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class PagamentoSecurityIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoBean
    private PedidoClient pedidoClient;

    private HttpHeaders criarHeadersAutenticado() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Auth-User-Email", "admin@alurafood.com");
        headers.set("X-Auth-User-Role", "ROLE_USER");
        headers.set("X-Gateway-Secret", "test-gateway-secret");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private PagamentoDto criarPagamentoDto() {
        PagamentoDto dto = new PagamentoDto();
        dto.setValor(new BigDecimal("100.00"));
        dto.setNome("Test User");
        dto.setNumero("1234567890123456");
        dto.setExpiracao("12/2030");
        dto.setCodigo("123");
        dto.setStatus(Status.CRIADO);
        dto.setPedidoId(1L);
        dto.setFormaDePagamentoId(1L);
        return dto;
    }

    @Test
    void deveRetornar401AoAcessarPagamentosSemAutenticacao() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/pagamentos", String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void deveRetornar401AoAcessarPagamentosSemHeaderDeAutenticacao() {
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/pagamentos", HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void deveRetornarPagamentosComHeaderDeAutenticacao() {
        HttpEntity<Void> entity = new HttpEntity<>(criarHeadersAutenticado());

        ResponseEntity<String> response = restTemplate.exchange(
                "/pagamentos", HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void deveCriarPagamentoComAutenticacao() {
        PagamentoDto dto = criarPagamentoDto();
        HttpEntity<PagamentoDto> entity = new HttpEntity<>(dto, criarHeadersAutenticado());

        ResponseEntity<PagamentoDto> response = restTemplate.exchange(
                "/pagamentos", HttpMethod.POST, entity, PagamentoDto.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
    }

    @Test
    void deveRetornar401AoCriarPagamentoSemAutenticacao() {
        PagamentoDto dto = criarPagamentoDto();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PagamentoDto> entity = new HttpEntity<>(dto, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/pagamentos", HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
