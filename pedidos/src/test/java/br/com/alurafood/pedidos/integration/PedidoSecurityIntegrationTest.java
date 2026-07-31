package br.com.alurafood.pedidos.integration;

import br.com.alurafood.pedidos.dto.ItemDoPedidoDto;
import br.com.alurafood.pedidos.dto.PedidoDto;
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
class PedidoSecurityIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private HttpHeaders criarHeadersAutenticado() {
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
    void deveRetornar401AoAcessarPedidosSemAutenticacao() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/pedidos", String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void deveRetornarPedidosComHeaderDeAutenticacao() {
        HttpEntity<Void> entity = new HttpEntity<>(criarHeadersAutenticado());

        ResponseEntity<String> response = restTemplate.exchange(
                "/pedidos", HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void deveCriarPedidoComAutenticacao() {
        PedidoDto dto = criarPedidoDto();
        HttpEntity<PedidoDto> entity = new HttpEntity<>(dto, criarHeadersAutenticado());

        ResponseEntity<PedidoDto> response = restTemplate.exchange(
                "/pedidos", HttpMethod.POST, entity, PedidoDto.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
    }

    @Test
    void deveRetornar401AoCriarPedidoSemAutenticacao() {
        PedidoDto dto = criarPedidoDto();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PedidoDto> entity = new HttpEntity<>(dto, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/pedidos", HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
