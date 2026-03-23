package br.com.alurafood.pedidos.controller;

import br.com.alurafood.pedidos.config.GatewayAuthFilter;
import br.com.alurafood.pedidos.config.SecurityConfig;
import br.com.alurafood.pedidos.dto.ItemDoPedidoDto;
import br.com.alurafood.pedidos.dto.PedidoDto;
import br.com.alurafood.pedidos.dto.StatusDto;
import br.com.alurafood.pedidos.model.Status;
import br.com.alurafood.pedidos.service.PedidoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PedidoController.class)
@Import({SecurityConfig.class, GatewayAuthFilter.class})
class PedidoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PedidoService service;

    @Autowired
    private ObjectMapper objectMapper;

    private PedidoDto pedidoDto;

    @BeforeEach
    void setUp() {
        pedidoDto = new PedidoDto();
        pedidoDto.setId(1L);
        pedidoDto.setDataHora(LocalDateTime.of(2024, 1, 1, 12, 0));
        pedidoDto.setStatus(Status.REALIZADO);

        ItemDoPedidoDto itemDto = new ItemDoPedidoDto();
        itemDto.setId(1L);
        itemDto.setQuantidade(2);
        itemDto.setDescricao("Pizza");
        pedidoDto.setItens(new ArrayList<>(List.of(itemDto)));
    }

    @Test
    void listarTodos_deveRetornarListaDePedidos() throws Exception {
        when(service.obterTodos()).thenReturn(List.of(pedidoDto));

        mockMvc.perform(get("/pedidos")
                        .header("X-Auth-User-Email", "admin@alurafood.com")
                        .header("X-Auth-User-Role", "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("REALIZADO"));
    }

    @Test
    void listarPorId_deveRetornarPedidoPorId() throws Exception {
        when(service.obterPorId(1L)).thenReturn(pedidoDto);

        mockMvc.perform(get("/pedidos/1")
                        .header("X-Auth-User-Email", "admin@alurafood.com")
                        .header("X-Auth-User-Role", "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("REALIZADO"));
    }

    @Test
    void realizaPedido_deveCriarNovoPedido() throws Exception {
        when(service.criarPedido(any(PedidoDto.class))).thenReturn(pedidoDto);

        mockMvc.perform(post("/pedidos")
                        .header("X-Auth-User-Email", "admin@alurafood.com")
                        .header("X-Auth-User-Role", "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pedidoDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("REALIZADO"));
    }

    @Test
    void atualizaStatus_deveAtualizarStatusDoPedido() throws Exception {
        StatusDto statusDto = new StatusDto();
        statusDto.setStatus(Status.CONFIRMADO);

        PedidoDto atualizado = new PedidoDto();
        atualizado.setId(1L);
        atualizado.setStatus(Status.CONFIRMADO);
        atualizado.setDataHora(LocalDateTime.of(2024, 1, 1, 12, 0));
        atualizado.setItens(new ArrayList<>());

        when(service.atualizaStatus(eq(1L), any(StatusDto.class))).thenReturn(atualizado);

        mockMvc.perform(put("/pedidos/1/status")
                        .header("X-Auth-User-Email", "admin@alurafood.com")
                        .header("X-Auth-User-Role", "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADO"));
    }

    @Test
    void aprovaPagamento_deveAprovarPagamentoDoPedido() throws Exception {
        doNothing().when(service).aprovaPagamentoPedido(1L);

        mockMvc.perform(put("/pedidos/1/pago")
                        .header("X-Auth-User-Email", "admin@alurafood.com")
                        .header("X-Auth-User-Role", "ROLE_USER"))
                .andExpect(status().isOk());

        verify(service).aprovaPagamentoPedido(1L);
    }
}
