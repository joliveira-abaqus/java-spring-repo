package br.com.alurafood.pagamentos.service;

import br.com.alurafood.pagamentos.dto.PagamentoDto;
import br.com.alurafood.pagamentos.http.PedidoClient;
import br.com.alurafood.pagamentos.model.Pagamento;
import br.com.alurafood.pagamentos.model.Status;
import br.com.alurafood.pagamentos.repository.PagamentoRepositoy;
import javax.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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

    @InjectMocks
    private PagamentoService service;

    @Mock
    private PagamentoRepositoy repository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private PedidoClient pedido;

    private Pagamento criarPagamento() {
        Pagamento p = new Pagamento();
        p.setId(1L);
        p.setValor(new BigDecimal("100.00"));
        p.setNome("Joao Silva");
        p.setNumero("1234567890123456");
        p.setExpiracao("12/2030");
        p.setCodigo("123");
        p.setStatus(Status.CRIADO);
        p.setPedidoId(1L);
        p.setFormaDePagamentoId(1L);
        return p;
    }

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
    void obterTodos_deveRetornarPaginaDePagamentos() {
        Pagamento pagamento = criarPagamento();
        PagamentoDto dto = criarPagamentoDto();
        Page<Pagamento> pagina = new PageImpl<>(List.of(pagamento));
        Pageable pageable = Pageable.unpaged();

        when(repository.findAll(pageable)).thenReturn(pagina);
        when(modelMapper.map(pagamento, PagamentoDto.class)).thenReturn(dto);

        Page<PagamentoDto> resultado = service.obterTodos(pageable);

        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
        assertEquals(dto, resultado.getContent().get(0));
        verify(repository).findAll(pageable);
    }

    @Test
    void obterPorId_quandoExiste_deveRetornarDto() {
        Pagamento pagamento = criarPagamento();
        PagamentoDto dto = criarPagamentoDto();

        when(repository.findById(1L)).thenReturn(Optional.of(pagamento));
        when(modelMapper.map(pagamento, PagamentoDto.class)).thenReturn(dto);

        PagamentoDto resultado = service.obterPorId(1L);

        assertNotNull(resultado);
        assertEquals(dto.getId(), resultado.getId());
        assertEquals(dto.getValor(), resultado.getValor());
        verify(repository).findById(1L);
    }

    @Test
    void obterPorId_quandoNaoExiste_deveLancarEntityNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.obterPorId(99L));
        verify(repository).findById(99L);
    }

    @Test
    void criarPagamento_deveSetarStatusCriado() {
        PagamentoDto dto = criarPagamentoDto();
        Pagamento pagamento = criarPagamento();
        Pagamento pagamentoSalvo = criarPagamento();
        pagamentoSalvo.setStatus(Status.CRIADO);

        when(modelMapper.map(dto, Pagamento.class)).thenReturn(pagamento);
        when(repository.save(pagamento)).thenReturn(pagamentoSalvo);
        when(modelMapper.map(pagamento, PagamentoDto.class)).thenReturn(dto);

        PagamentoDto resultado = service.criarPagamento(dto);

        assertNotNull(resultado);
        assertEquals(Status.CRIADO, pagamento.getStatus());
        verify(repository).save(pagamento);
    }

    @Test
    void atualizarPagamento_deveRetornarDtoAtualizado() {
        PagamentoDto dto = criarPagamentoDto();
        Pagamento pagamento = criarPagamento();
        PagamentoDto dtoAtualizado = criarPagamentoDto();
        dtoAtualizado.setNome("Maria Silva");

        when(modelMapper.map(dto, Pagamento.class)).thenReturn(pagamento);
        when(repository.save(pagamento)).thenReturn(pagamento);
        when(modelMapper.map(pagamento, PagamentoDto.class)).thenReturn(dtoAtualizado);

        PagamentoDto resultado = service.atualizarPagamento(1L, dto);

        assertNotNull(resultado);
        assertEquals("Maria Silva", resultado.getNome());
        verify(repository).save(pagamento);
    }

    @Test
    void excluirPagamento_deveChamarDeleteById() {
        doNothing().when(repository).deleteById(1L);

        service.excluirPagamento(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void confirmarPagamento_deveSetarStatusConfirmadoEChamarPedidoClient() {
        Pagamento pagamento = criarPagamento();

        when(repository.findById(1L)).thenReturn(Optional.of(pagamento));

        service.confirmarPagamento(1L);

        assertEquals(Status.CONFIRMADO, pagamento.getStatus());
        verify(repository).save(pagamento);
        verify(pedido).atualizaPagamento(pagamento.getPedidoId());
    }

    @Test
    void confirmarPagamento_quandoNaoExiste_deveLancarEntityNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.confirmarPagamento(99L));
        verify(repository, never()).save(any());
        verify(pedido, never()).atualizaPagamento(any());
    }

    @Test
    void alteraStatus_deveSetarStatusConfirmadoSemIntegracao() {
        Pagamento pagamento = criarPagamento();

        when(repository.findById(1L)).thenReturn(Optional.of(pagamento));

        service.alteraStatus(1L);

        assertEquals(Status.CONFIRMADO_SEM_INTEGRACAO, pagamento.getStatus());
        verify(repository).save(pagamento);
    }
}
