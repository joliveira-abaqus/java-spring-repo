package br.com.alurafood.pedidos.controller;

import br.com.alurafood.pedidos.dto.ItemDoPedidoDto;
import br.com.alurafood.pedidos.dto.PedidoDto;
import br.com.alurafood.pedidos.dto.StatusDto;
import br.com.alurafood.pedidos.model.Status;
import br.com.alurafood.pedidos.service.PedidoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PedidoController.class)
class PedidoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PedidoService service;

    @Autowired
    private ObjectMapper objectMapper;

    private PedidoDto criarPedidoDto() {
        PedidoDto dto = new PedidoDto();
        dto.setId(1L);
        dto.setDataHora(LocalDateTime.of(2026, 3, 11, 10, 0));
        dto.setStatus(Status.REALIZADO);
        List<ItemDoPedidoDto> itens = new ArrayList<>();
        ItemDoPedidoDto itemDto = new ItemDoPedidoDto();
        itemDto.setId(1L);
        itemDto.setQuantidade(2);
        itemDto.setDescricao("Pizza Margherita");
        itens.add(itemDto);
        dto.setItens(itens);
        return dto;
    }

    @Test
    void listarTodos_deveRetornarListaDePedidos() throws Exception {
        PedidoDto dto = criarPedidoDto();

        when(service.obterTodos()).thenReturn(List.of(dto));

        mockMvc.perform(get("/pedidos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("REALIZADO"));

        verify(service).obterTodos();
    }

    @Test
    void listarPorId_quandoExiste_deveRetornarOk() throws Exception {
        PedidoDto dto = criarPedidoDto();

        when(service.obterPorId(1L)).thenReturn(dto);

        mockMvc.perform(get("/pedidos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("REALIZADO"))
                .andExpect(jsonPath("$.itens[0].descricao").value("Pizza Margherita"));

        verify(service).obterPorId(1L);
    }

    @Test
    void listarPorId_quandoNaoExiste_deveLancarExcecao() throws Exception {
        when(service.obterPorId(99L)).thenThrow(new EntityNotFoundException());

        assertThrows(Exception.class, () ->
                mockMvc.perform(get("/pedidos/99"))
        );

        verify(service).obterPorId(99L);
    }

    @Test
    void realizaPedido_deveRetornarCreated() throws Exception {
        PedidoDto dto = criarPedidoDto();

        when(service.criarPedido(any(PedidoDto.class))).thenReturn(dto);

        mockMvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("REALIZADO"));

        verify(service).criarPedido(any(PedidoDto.class));
    }

    @Test
    void atualizaStatus_deveRetornarOk() throws Exception {
        PedidoDto dto = criarPedidoDto();
        dto.setStatus(Status.CONFIRMADO);
        StatusDto statusDto = new StatusDto(Status.CONFIRMADO);

        when(service.atualizaStatus(eq(1L), any(StatusDto.class))).thenReturn(dto);

        mockMvc.perform(put("/pedidos/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADO"));

        verify(service).atualizaStatus(eq(1L), any(StatusDto.class));
    }

    @Test
    void aprovaPagamento_deveRetornarOk() throws Exception {
        doNothing().when(service).aprovaPagamentoPedido(1L);

        mockMvc.perform(put("/pedidos/1/pago"))
                .andExpect(status().isOk());

        verify(service).aprovaPagamentoPedido(1L);
    }
}
