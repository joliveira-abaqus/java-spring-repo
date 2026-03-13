package br.com.alurafood.pedidos.integration;

import br.com.alurafood.pedidos.dto.ItemDoPedidoDto;
import br.com.alurafood.pedidos.dto.PedidoDto;
import br.com.alurafood.pedidos.dto.StatusDto;
import br.com.alurafood.pedidos.model.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PedidoIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private PedidoDto criarPedidoDto() {
        PedidoDto dto = new PedidoDto();
        dto.setStatus(Status.REALIZADO);

        ItemDoPedidoDto item = new ItemDoPedidoDto();
        item.setQuantidade(2);
        item.setDescricao("Pizza Margherita");
        dto.setItens(new ArrayList<>(List.of(item)));

        return dto;
    }

    @Test
    void deveCriarPedidoComSucesso() {
        PedidoDto dto = criarPedidoDto();

        ResponseEntity<PedidoDto> response = restTemplate.postForEntity(
                "/pedidos", dto, PedidoDto.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals(Status.REALIZADO, response.getBody().getStatus());
    }

    @Test
    void deveListarTodosOsPedidos() {
        PedidoDto dto = criarPedidoDto();
        restTemplate.postForEntity("/pedidos", dto, PedidoDto.class);

        ResponseEntity<PedidoDto[]> response = restTemplate.getForEntity(
                "/pedidos", PedidoDto[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);
    }

    @Test
    void deveConsultarPedidoPorId() {
        PedidoDto dto = criarPedidoDto();
        ResponseEntity<PedidoDto> criado = restTemplate.postForEntity(
                "/pedidos", dto, PedidoDto.class);

        Long id = criado.getBody().getId();

        ResponseEntity<PedidoDto> response = restTemplate.getForEntity(
                "/pedidos/" + id, PedidoDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(id, response.getBody().getId());
    }

    @Test
    void deveAtualizarStatusDoPedido() {
        PedidoDto dto = criarPedidoDto();
        ResponseEntity<PedidoDto> criado = restTemplate.postForEntity(
                "/pedidos", dto, PedidoDto.class);

        Long id = criado.getBody().getId();

        StatusDto statusDto = new StatusDto();
        statusDto.setStatus(Status.CONFIRMADO);

        ResponseEntity<PedidoDto> response = restTemplate.exchange(
                "/pedidos/" + id + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(statusDto),
                PedidoDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Status.CONFIRMADO, response.getBody().getStatus());
    }

    @Test
    void deveAprovarPagamentoDoPedido() {
        PedidoDto dto = criarPedidoDto();
        ResponseEntity<PedidoDto> criado = restTemplate.postForEntity(
                "/pedidos", dto, PedidoDto.class);

        Long id = criado.getBody().getId();

        ResponseEntity<Void> response = restTemplate.exchange(
                "/pedidos/" + id + "/pago",
                HttpMethod.PUT,
                null,
                Void.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        ResponseEntity<PedidoDto> consultado = restTemplate.getForEntity(
                "/pedidos/" + id, PedidoDto.class);

        assertEquals(Status.PAGO, consultado.getBody().getStatus());
    }
}
