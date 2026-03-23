package br.com.alurafood.pagamentos.integration;

import br.com.alurafood.pagamentos.dto.PagamentoDto;
import br.com.alurafood.pagamentos.http.PedidoClient;
import br.com.alurafood.pagamentos.model.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doNothing;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PagamentoIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoBean
    private PedidoClient pedidoClient;

    private HttpHeaders criarHeaders() {
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
    void deveCriarPagamentoComSucesso() {
        PagamentoDto dto = criarPagamentoDto();
        HttpEntity<PagamentoDto> entity = new HttpEntity<>(dto, criarHeaders());

        ResponseEntity<PagamentoDto> response = restTemplate.exchange(
                "/pagamentos", HttpMethod.POST, entity, PagamentoDto.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals(Status.CRIADO, response.getBody().getStatus());
    }

    @Test
    void deveConsultarPagamentoPorId() {
        PagamentoDto dto = criarPagamentoDto();
        HttpEntity<PagamentoDto> createEntity = new HttpEntity<>(dto, criarHeaders());
        ResponseEntity<PagamentoDto> criado = restTemplate.exchange(
                "/pagamentos", HttpMethod.POST, createEntity, PagamentoDto.class);

        Long id = criado.getBody().getId();

        HttpEntity<Void> getEntity = new HttpEntity<>(criarHeaders());
        ResponseEntity<PagamentoDto> response = restTemplate.exchange(
                "/pagamentos/" + id, HttpMethod.GET, getEntity, PagamentoDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(id, response.getBody().getId());
    }

    @Test
    void deveConfirmarPagamentoComSucesso() {
        doNothing().when(pedidoClient).atualizaPagamento(1L);

        PagamentoDto dto = criarPagamentoDto();
        HttpEntity<PagamentoDto> createEntity = new HttpEntity<>(dto, criarHeaders());
        ResponseEntity<PagamentoDto> criado = restTemplate.exchange(
                "/pagamentos", HttpMethod.POST, createEntity, PagamentoDto.class);

        Long id = criado.getBody().getId();

        HttpEntity<Void> confirmEntity = new HttpEntity<>(criarHeaders());
        ResponseEntity<Void> confirmResponse = restTemplate.exchange(
                "/pagamentos/" + id + "/confirmar",
                HttpMethod.PATCH,
                confirmEntity,
                Void.class);

        assertEquals(HttpStatus.OK, confirmResponse.getStatusCode());

        HttpEntity<Void> getEntity = new HttpEntity<>(criarHeaders());
        ResponseEntity<PagamentoDto> consultado = restTemplate.exchange(
                "/pagamentos/" + id, HttpMethod.GET, getEntity, PagamentoDto.class);

        assertEquals(Status.CONFIRMADO, consultado.getBody().getStatus());
    }

    @Test
    void deveAtualizarPagamento() {
        PagamentoDto dto = criarPagamentoDto();
        HttpEntity<PagamentoDto> createEntity = new HttpEntity<>(dto, criarHeaders());
        ResponseEntity<PagamentoDto> criado = restTemplate.exchange(
                "/pagamentos", HttpMethod.POST, createEntity, PagamentoDto.class);

        Long id = criado.getBody().getId();

        PagamentoDto atualizado = criarPagamentoDto();
        atualizado.setValor(new BigDecimal("200.00"));
        atualizado.setNome("Updated User");

        HttpEntity<PagamentoDto> updateEntity = new HttpEntity<>(atualizado, criarHeaders());
        ResponseEntity<PagamentoDto> response = restTemplate.exchange(
                "/pagamentos/" + id,
                HttpMethod.PUT,
                updateEntity,
                PagamentoDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new BigDecimal("200.00"), response.getBody().getValor());
    }

    @Test
    void deveExcluirPagamento() {
        PagamentoDto dto = criarPagamentoDto();
        HttpEntity<PagamentoDto> createEntity = new HttpEntity<>(dto, criarHeaders());
        ResponseEntity<PagamentoDto> criado = restTemplate.exchange(
                "/pagamentos", HttpMethod.POST, createEntity, PagamentoDto.class);

        Long id = criado.getBody().getId();

        HttpEntity<Void> deleteEntity = new HttpEntity<>(criarHeaders());
        restTemplate.exchange("/pagamentos/" + id, HttpMethod.DELETE, deleteEntity, Void.class);

        HttpEntity<Void> getEntity = new HttpEntity<>(criarHeaders());
        ResponseEntity<String> response = restTemplate.exchange(
                "/pagamentos/" + id, HttpMethod.GET, getEntity, String.class);

        // After deletion, getting the resource should return an error
        assertNotEquals(HttpStatus.OK, response.getStatusCode());
    }
}
