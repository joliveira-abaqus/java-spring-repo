package br.com.alurafood.pagamentos.controller;

import br.com.alurafood.pagamentos.config.GatewayAuthFilter;
import br.com.alurafood.pagamentos.config.SecurityConfig;
import br.com.alurafood.pagamentos.dto.PagamentoDto;
import br.com.alurafood.pagamentos.model.Status;
import br.com.alurafood.pagamentos.service.PagamentoService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PagamentoController.class)
@Import({SecurityConfig.class, GatewayAuthFilter.class})
@ActiveProfiles("test")
class PagamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PagamentoService service;

    @Autowired
    private ObjectMapper objectMapper;

    private PagamentoDto pagamentoDto;

    @BeforeEach
    void setUp() {
        pagamentoDto = new PagamentoDto();
        pagamentoDto.setId(1L);
        pagamentoDto.setValor(new BigDecimal("100.00"));
        pagamentoDto.setNome("Test User");
        pagamentoDto.setNumero("1234567890123456");
        pagamentoDto.setExpiracao("12/2030");
        pagamentoDto.setCodigo("123");
        pagamentoDto.setStatus(Status.CRIADO);
        pagamentoDto.setPedidoId(1L);
        pagamentoDto.setFormaDePagamentoId(1L);
    }

    @Test
    void listar_deveRetornarPaginaDePagamentos() throws Exception {
        Page<PagamentoDto> page = new PageImpl<>(List.of(pagamentoDto));
        when(service.obterTodos(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/pagamentos")
                        .header("X-Auth-User-Email", "admin@alurafood.com")
                        .header("X-Auth-User-Role", "ROLE_USER")
                        .header("X-Gateway-Secret", "test-gateway-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void detalhar_deveRetornarPagamentoPorId() throws Exception {
        when(service.obterPorId(1L)).thenReturn(pagamentoDto);

        mockMvc.perform(get("/pagamentos/1")
                        .header("X-Auth-User-Email", "admin@alurafood.com")
                        .header("X-Auth-User-Role", "ROLE_USER")
                        .header("X-Gateway-Secret", "test-gateway-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Test User"));
    }

    @Test
    void cadastrar_deveCriarNovoPagamento() throws Exception {
        when(service.criarPagamento(any(PagamentoDto.class))).thenReturn(pagamentoDto);

        mockMvc.perform(post("/pagamentos")
                        .header("X-Auth-User-Email", "admin@alurafood.com")
                        .header("X-Auth-User-Role", "ROLE_USER")
                        .header("X-Gateway-Secret", "test-gateway-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pagamentoDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CRIADO"));
    }

    @Test
    void atualizar_deveAtualizarPagamento() throws Exception {
        when(service.atualizarPagamento(eq(1L), any(PagamentoDto.class))).thenReturn(pagamentoDto);

        mockMvc.perform(put("/pagamentos/1")
                        .header("X-Auth-User-Email", "admin@alurafood.com")
                        .header("X-Auth-User-Role", "ROLE_USER")
                        .header("X-Gateway-Secret", "test-gateway-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pagamentoDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void remover_deveRemoverPagamento() throws Exception {
        doNothing().when(service).excluirPagamento(1L);

        mockMvc.perform(delete("/pagamentos/1")
                        .header("X-Auth-User-Email", "admin@alurafood.com")
                        .header("X-Auth-User-Role", "ROLE_USER")
                        .header("X-Gateway-Secret", "test-gateway-secret"))
                .andExpect(status().isNoContent());

        verify(service).excluirPagamento(1L);
    }

    @Test
    void confirmarPagamento_deveConfirmarPagamento() throws Exception {
        doNothing().when(service).confirmarPagamento(1L);

        mockMvc.perform(patch("/pagamentos/1/confirmar")
                        .header("X-Auth-User-Email", "admin@alurafood.com")
                        .header("X-Auth-User-Role", "ROLE_USER")
                        .header("X-Gateway-Secret", "test-gateway-secret"))
                .andExpect(status().isOk());

        verify(service).confirmarPagamento(1L);
    }
}
