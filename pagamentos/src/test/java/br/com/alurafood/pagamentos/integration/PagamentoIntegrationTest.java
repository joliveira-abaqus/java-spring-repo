package br.com.alurafood.pagamentos.integration;

import br.com.alurafood.pagamentos.dto.PagamentoDto;
import br.com.alurafood.pagamentos.http.PedidoClient;
import br.com.alurafood.pagamentos.model.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

        ResponseEntity<PagamentoDto> response = restTemplate.postForEntity(
                "/pagamentos", dto, PagamentoDto.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals(Status.CRIADO, response.getBody().getStatus());
    }

    @Test
    void deveConsultarPagamentoPorId() {
        PagamentoDto dto = criarPagamentoDto();
        ResponseEntity<PagamentoDto> criado = restTemplate.postForEntity(
                "/pagamentos", dto, PagamentoDto.class);

        Long id = criado.getBody().getId();

        ResponseEntity<PagamentoDto> response = restTemplate.getForEntity(
                "/pagamentos/" + id, PagamentoDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(id, response.getBody().getId());
    }

    @Test
    void deveConfirmarPagamentoComSucesso() {
        doNothing().when(pedidoClient).atualizaPagamento(1L);

        PagamentoDto dto = criarPagamentoDto();
        ResponseEntity<PagamentoDto> criado = restTemplate.postForEntity(
                "/pagamentos", dto, PagamentoDto.class);

        Long id = criado.getBody().getId();

        ResponseEntity<Void> confirmResponse = restTemplate.exchange(
                "/pagamentos/" + id + "/confirmar",
                HttpMethod.PATCH,
                null,
                Void.class);

        assertEquals(HttpStatus.OK, confirmResponse.getStatusCode());

        ResponseEntity<PagamentoDto> consultado = restTemplate.getForEntity(
                "/pagamentos/" + id, PagamentoDto.class);

        assertEquals(Status.CONFIRMADO, consultado.getBody().getStatus());
    }

    @Test
    void deveAtualizarPagamento() {
        PagamentoDto dto = criarPagamentoDto();
        ResponseEntity<PagamentoDto> criado = restTemplate.postForEntity(
                "/pagamentos", dto, PagamentoDto.class);

        Long id = criado.getBody().getId();

        PagamentoDto atualizado = criarPagamentoDto();
        atualizado.setValor(new BigDecimal("200.00"));
        atualizado.setNome("Updated User");

        ResponseEntity<PagamentoDto> response = restTemplate.exchange(
                "/pagamentos/" + id,
                HttpMethod.PUT,
                new HttpEntity<>(atualizado),
                PagamentoDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new BigDecimal("200.00"), response.getBody().getValor());
    }

    @Test
    void deveExcluirPagamento() {
        PagamentoDto dto = criarPagamentoDto();
        ResponseEntity<PagamentoDto> criado = restTemplate.postForEntity(
                "/pagamentos", dto, PagamentoDto.class);

        Long id = criado.getBody().getId();

        restTemplate.delete("/pagamentos/" + id);

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/pagamentos/" + id, String.class);

        // After deletion, getting the resource should return an error
        assertNotEquals(HttpStatus.OK, response.getStatusCode());
    }
}
