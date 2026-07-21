package br.com.alurafood.pedidos.controller;

import br.com.alurafood.pedidos.service.PedidoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class PedidoControllerUnitTest {

    @Mock
    private PedidoService service;

    @InjectMocks
    private PedidoController controller;

    @Test
    void retornaPorta_deveFormatarMensagemComPorta() {
        String resultado = controller.retornaPorta("8084");

        assertEquals("Requisição respondida pela instância executando na porta 8084", resultado);
    }

    @Test
    void retornaPorta_deveIncluirPortaInformada() {
        String resultado = controller.retornaPorta("9999");

        assertTrue(resultado.contains("9999"));
    }
}
