package br.com.alurafood.pagamentos.controller;

import br.com.alurafood.pagamentos.dto.PagamentoDto;
import br.com.alurafood.pagamentos.model.Status;
import br.com.alurafood.pagamentos.service.PagamentoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PagamentoController.class)
class PagamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PagamentoService service;

    @Autowired
    private ObjectMapper objectMapper;

    private PagamentoDto criarPagamentoDto() {
        PagamentoDto dto = new PagamentoDto();
        dto.setId(1L);
        dto.setValor(new BigDecimal("100.00"));
        dto.setNome("Joao Silva");
        dto.setNumero("1234567890123456");
        dto.setExpiracao("12/2030");
        dto.setCodigo("123");
        dto.setStatus(Status.CRIADO);
        dto.setPedidoId(1L);
        dto.setFormaDePagamentoId(1L);
        return dto;
    }

    @Test
    void listar_deveRetornarPaginaDePagamentos() throws Exception {
        PagamentoDto dto = criarPagamentoDto();
        Page<PagamentoDto> pagina = new PageImpl<>(List.of(dto));

        when(service.obterTodos(any(Pageable.class))).thenReturn(pagina);

        mockMvc.perform(get("/pagamentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].nome").value("Joao Silva"));

        verify(service).obterTodos(any(Pageable.class));
    }

    @Test
    void detalhar_quandoExiste_deveRetornarOk() throws Exception {
        PagamentoDto dto = criarPagamentoDto();

        when(service.obterPorId(1L)).thenReturn(dto);

        mockMvc.perform(get("/pagamentos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Joao Silva"))
                .andExpect(jsonPath("$.valor").value(100.00));

        verify(service).obterPorId(1L);
    }

    @Test
    void detalhar_quandoNaoExiste_deveLancarExcecao() throws Exception {
        when(service.obterPorId(99L)).thenThrow(new EntityNotFoundException());

        assertThrows(Exception.class, () ->
                mockMvc.perform(get("/pagamentos/99"))
        );

        verify(service).obterPorId(99L);
    }

    @Test
    void cadastrar_deveRetornarCreated() throws Exception {
        PagamentoDto dto = criarPagamentoDto();

        when(service.criarPagamento(any(PagamentoDto.class))).thenReturn(dto);

        mockMvc.perform(post("/pagamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CRIADO"));

        verify(service).criarPagamento(any(PagamentoDto.class));
    }

    @Test
    void atualizar_deveRetornarOk() throws Exception {
        PagamentoDto dto = criarPagamentoDto();
        dto.setNome("Maria Silva");

        when(service.atualizarPagamento(eq(1L), any(PagamentoDto.class))).thenReturn(dto);

        mockMvc.perform(put("/pagamentos/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Silva"));

        verify(service).atualizarPagamento(eq(1L), any(PagamentoDto.class));
    }

    @Test
    void remover_deveRetornarNoContent() throws Exception {
        doNothing().when(service).excluirPagamento(1L);

        mockMvc.perform(delete("/pagamentos/1"))
                .andExpect(status().isNoContent());

        verify(service).excluirPagamento(1L);
    }

    @Test
    void confirmarPagamento_deveRetornarOk() throws Exception {
        doNothing().when(service).confirmarPagamento(1L);

        mockMvc.perform(patch("/pagamentos/1/confirmar"))
                .andExpect(status().isOk());

        verify(service).confirmarPagamento(1L);
    }
}
