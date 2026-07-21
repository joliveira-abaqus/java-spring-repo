package br.com.alurafood.pagamentos.controller;

import br.com.alurafood.pagamentos.service.PagamentoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PagamentoControllerUnitTest {

    @Mock
    private PagamentoService service;

    @InjectMocks
    private PagamentoController controller;

    @BeforeEach
    void setUp() {
    }

    @Test
    void confirmarPagamento_deveDelegarParaService() {
        controller.confirmarPagamento(1L);

        verify(service).confirmarPagamento(1L);
    }

    @Test
    void pagamentoAutorizadoComIntegracaoPendente_deveAlterarStatusNoFallback() {
        controller.pagamentoAutorizadoComIntegracaoPendente(1L, new RuntimeException("indisponivel"));

        verify(service).alteraStatus(1L);
    }
}
