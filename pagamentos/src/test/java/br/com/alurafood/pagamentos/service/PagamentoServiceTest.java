package br.com.alurafood.pagamentos.service;

import br.com.alurafood.pagamentos.dto.PagamentoDto;
import br.com.alurafood.pagamentos.http.PedidoClient;
import br.com.alurafood.pagamentos.model.Pagamento;
import br.com.alurafood.pagamentos.model.Status;
import br.com.alurafood.pagamentos.repository.PagamentoRepositoy;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagamentoServiceTest {

    @Mock
    private PagamentoRepositoy repository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private PedidoClient pedido;

    @InjectMocks
    private PagamentoService service;

    private Pagamento pagamento;
    private PagamentoDto pagamentoDto;

    @BeforeEach
    void setUp() {
        pagamento = new Pagamento();
        pagamento.setId(1L);
        pagamento.setValor(new BigDecimal("100.00"));
        pagamento.setNome("Test User");
        pagamento.setNumero("1234567890123456");
        pagamento.setExpiracao("12/2030");
        pagamento.setCodigo("123");
        pagamento.setStatus(Status.CRIADO);
        pagamento.setPedidoId(1L);
        pagamento.setFormaDePagamentoId(1L);

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
    void obterTodos_deveRetornarPaginaDePagamentos() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Pagamento> page = new PageImpl<>(List.of(pagamento));

        when(repository.findAll(pageable)).thenReturn(page);
        when(modelMapper.map(any(Pagamento.class), eq(PagamentoDto.class))).thenReturn(pagamentoDto);

        Page<PagamentoDto> result = service.obterTodos(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(repository).findAll(pageable);
    }

    @Test
    void obterPorId_deveRetornarPagamentoQuandoExiste() {
        when(repository.findById(1L)).thenReturn(Optional.of(pagamento));
        when(modelMapper.map(pagamento, PagamentoDto.class)).thenReturn(pagamentoDto);

        PagamentoDto result = service.obterPorId(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(repository).findById(1L);
    }

    @Test
    void obterPorId_deveLancarExcecaoQuandoNaoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.obterPorId(99L));
    }

    @Test
    void criarPagamento_deveCriarComStatusCriado() {
        Pagamento pag = new Pagamento();
        pag.setId(1L);

        when(modelMapper.map(pagamentoDto, Pagamento.class)).thenReturn(pag);
        when(repository.save(pag)).thenReturn(pag);
        when(modelMapper.map(pag, PagamentoDto.class)).thenReturn(pagamentoDto);

        PagamentoDto result = service.criarPagamento(pagamentoDto);

        assertNotNull(result);
        assertEquals(Status.CRIADO, pag.getStatus());
        verify(repository).save(pag);
    }

    @Test
    void atualizarPagamento_deveAtualizarPagamento() {
        Pagamento pag = new Pagamento();
        when(modelMapper.map(pagamentoDto, Pagamento.class)).thenReturn(pag);
        when(repository.save(pag)).thenReturn(pag);
        when(modelMapper.map(pag, PagamentoDto.class)).thenReturn(pagamentoDto);

        PagamentoDto result = service.atualizarPagamento(1L, pagamentoDto);

        assertNotNull(result);
        assertEquals(1L, pag.getId());
        verify(repository).save(pag);
    }

    @Test
    void excluirPagamento_deveExcluirPagamento() {
        doNothing().when(repository).deleteById(1L);

        service.excluirPagamento(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void confirmarPagamento_deveConfirmarEAtualizarPedido() {
        when(repository.findById(1L)).thenReturn(Optional.of(pagamento));
        when(repository.save(pagamento)).thenReturn(pagamento);

        service.confirmarPagamento(1L);

        assertEquals(Status.CONFIRMADO, pagamento.getStatus());
        verify(repository).save(pagamento);
        verify(pedido).atualizaPagamento(1L);
    }

    @Test
    void confirmarPagamento_deveLancarExcecaoQuandoNaoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.confirmarPagamento(99L));
    }

    @Test
    void alteraStatus_deveAlterarParaConfirmadoSemIntegracao() {
        when(repository.findById(1L)).thenReturn(Optional.of(pagamento));
        when(repository.save(pagamento)).thenReturn(pagamento);

        service.alteraStatus(1L);

        assertEquals(Status.CONFIRMADO_SEM_INTEGRACAO, pagamento.getStatus());
        verify(repository).save(pagamento);
    }

    @Test
    void alteraStatus_deveLancarExcecaoQuandoNaoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.alteraStatus(99L));
    }
}
