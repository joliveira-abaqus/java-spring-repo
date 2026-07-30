package br.com.alurafood.pedidos.integration;

import br.com.alurafood.pedidos.dto.ItemDoPedidoDto;
import br.com.alurafood.pedidos.dto.PedidoDto;
import br.com.alurafood.pedidos.dto.StatusDto;
import br.com.alurafood.pedidos.model.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class PedidoIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private HttpHeaders criarHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Auth-User-Email", "admin@alurafood.com");
        headers.set("X-Auth-User-Role", "ROLE_USER");
        headers.set("X-Gateway-Secret", "test-gateway-secret");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

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
        HttpEntity<PedidoDto> entity = new HttpEntity<>(dto, criarHeaders());

        ResponseEntity<PedidoDto> response = restTemplate.exchange(
                "/pedidos", HttpMethod.POST, entity, PedidoDto.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals(Status.REALIZADO, response.getBody().getStatus());
    }

    @Test
    void deveListarTodosOsPedidos() {
        PedidoDto dto = criarPedidoDto();
        HttpEntity<PedidoDto> createEntity = new HttpEntity<>(dto, criarHeaders());
        restTemplate.exchange("/pedidos", HttpMethod.POST, createEntity, PedidoDto.class);

        HttpEntity<Void> getEntity = new HttpEntity<>(criarHeaders());
        ResponseEntity<PedidoDto[]> response = restTemplate.exchange(
                "/pedidos", HttpMethod.GET, getEntity, PedidoDto[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);
    }

    @Test
    void deveConsultarPedidoPorId() {
        PedidoDto dto = criarPedidoDto();
        HttpEntity<PedidoDto> createEntity = new HttpEntity<>(dto, criarHeaders());
        ResponseEntity<PedidoDto> criado = restTemplate.exchange(
                "/pedidos", HttpMethod.POST, createEntity, PedidoDto.class);

        Long id = criado.getBody().getId();

        HttpEntity<Void> getEntity = new HttpEntity<>(criarHeaders());
        ResponseEntity<PedidoDto> response = restTemplate.exchange(
                "/pedidos/" + id, HttpMethod.GET, getEntity, PedidoDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(id, response.getBody().getId());
    }

    @Test
    void deveAtualizarStatusDoPedido() {
        PedidoDto dto = criarPedidoDto();
        HttpEntity<PedidoDto> createEntity = new HttpEntity<>(dto, criarHeaders());
        ResponseEntity<PedidoDto> criado = restTemplate.exchange(
                "/pedidos", HttpMethod.POST, createEntity, PedidoDto.class);

        Long id = criado.getBody().getId();

        StatusDto statusDto = new StatusDto();
        statusDto.setStatus(Status.CONFIRMADO);

        HttpEntity<StatusDto> statusEntity = new HttpEntity<>(statusDto, criarHeaders());
        ResponseEntity<PedidoDto> response = restTemplate.exchange(
                "/pedidos/" + id + "/status",
                HttpMethod.PUT,
                statusEntity,
                PedidoDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Status.CONFIRMADO, response.getBody().getStatus());
    }

    @Test
    void deveAprovarPagamentoDoPedido() {
        PedidoDto dto = criarPedidoDto();
        HttpEntity<PedidoDto> createEntity = new HttpEntity<>(dto, criarHeaders());
        ResponseEntity<PedidoDto> criado = restTemplate.exchange(
                "/pedidos", HttpMethod.POST, createEntity, PedidoDto.class);

        Long id = criado.getBody().getId();

        HttpEntity<Void> pagoEntity = new HttpEntity<>(criarHeaders());
        ResponseEntity<Void> response = restTemplate.exchange(
                "/pedidos/" + id + "/pago",
                HttpMethod.PUT,
                pagoEntity,
                Void.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        HttpEntity<Void> getEntity = new HttpEntity<>(criarHeaders());
        ResponseEntity<PedidoDto> consultado = restTemplate.exchange(
                "/pedidos/" + id, HttpMethod.GET, getEntity, PedidoDto.class);

        assertEquals(Status.PAGO, consultado.getBody().getStatus());
    }
}
