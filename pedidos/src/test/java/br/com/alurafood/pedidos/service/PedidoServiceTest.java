package br.com.alurafood.pedidos.service;

import br.com.alurafood.pedidos.dto.ItemDoPedidoDto;
import br.com.alurafood.pedidos.dto.PedidoDto;
import br.com.alurafood.pedidos.dto.StatusDto;
import br.com.alurafood.pedidos.model.ItemDoPedido;
import br.com.alurafood.pedidos.model.Pedido;
import br.com.alurafood.pedidos.model.Status;
import br.com.alurafood.pedidos.repository.PedidoRepository;
import javax.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    private PedidoService service;

    @Mock
    private PedidoRepository repository;

    @Mock
    private ModelMapper modelMapper;

    @BeforeEach
    void setUp() throws Exception {
        service = new PedidoService(modelMapper);
        Field repoField = PedidoService.class.getDeclaredField("repository");
        repoField.setAccessible(true);
        repoField.set(service, repository);
    }

    private Pedido criarPedido() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setDataHora(LocalDateTime.of(2026, 3, 11, 10, 0));
        pedido.setStatus(Status.REALIZADO);
        List<ItemDoPedido> itens = new ArrayList<>();
        ItemDoPedido item = new ItemDoPedido();
        item.setId(1L);
        item.setQuantidade(2);
        item.setDescricao("Pizza Margherita");
        item.setPedido(pedido);
        itens.add(item);
        pedido.setItens(itens);
        return pedido;
    }

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
    void obterTodos_deveRetornarListaDePedidos() {
        Pedido pedido = criarPedido();
        PedidoDto dto = criarPedidoDto();

        when(repository.findAll()).thenReturn(List.of(pedido));
        when(modelMapper.map(pedido, PedidoDto.class)).thenReturn(dto);

        List<PedidoDto> resultado = service.obterTodos();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals(dto.getId(), resultado.get(0).getId());
        verify(repository).findAll();
    }

    @Test
    void obterPorId_quandoExiste_deveRetornarDto() {
        Pedido pedido = criarPedido();
        PedidoDto dto = criarPedidoDto();

        when(repository.findById(1L)).thenReturn(Optional.of(pedido));
        when(modelMapper.map(pedido, PedidoDto.class)).thenReturn(dto);

        PedidoDto resultado = service.obterPorId(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals(Status.REALIZADO, resultado.getStatus());
        verify(repository).findById(1L);
    }

    @Test
    void obterPorId_quandoNaoExiste_deveLancarEntityNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.obterPorId(99L));
        verify(repository).findById(99L);
    }

    @Test
    void criarPedido_deveSetarStatusRealizadoEDataHora() {
        PedidoDto dto = criarPedidoDto();
        Pedido pedido = criarPedido();

        when(modelMapper.map(dto, Pedido.class)).thenReturn(pedido);
        when(repository.save(pedido)).thenReturn(pedido);
        when(modelMapper.map(pedido, PedidoDto.class)).thenReturn(dto);

        PedidoDto resultado = service.criarPedido(dto);

        assertNotNull(resultado);
        assertEquals(Status.REALIZADO, pedido.getStatus());
        assertNotNull(pedido.getDataHora());
        verify(repository).save(pedido);
    }

    @Test
    void atualizaStatus_quandoPedidoExiste_deveAtualizarStatus() {
        Pedido pedido = criarPedido();
        PedidoDto dto = criarPedidoDto();
        dto.setStatus(Status.CONFIRMADO);
        StatusDto statusDto = new StatusDto(Status.CONFIRMADO);

        when(repository.porIdComItens(1L)).thenReturn(pedido);
        when(modelMapper.map(pedido, PedidoDto.class)).thenReturn(dto);

        PedidoDto resultado = service.atualizaStatus(1L, statusDto);

        assertNotNull(resultado);
        assertEquals(Status.CONFIRMADO, pedido.getStatus());
        verify(repository).atualizaStatus(Status.CONFIRMADO, pedido);
    }

    @Test
    void atualizaStatus_quandoPedidoNaoExiste_deveLancarEntityNotFoundException() {
        StatusDto statusDto = new StatusDto(Status.CONFIRMADO);

        when(repository.porIdComItens(99L)).thenReturn(null);

        assertThrows(EntityNotFoundException.class, () -> service.atualizaStatus(99L, statusDto));
        verify(repository, never()).atualizaStatus(any(), any());
    }

    @Test
    void aprovaPagamentoPedido_quandoPedidoExiste_deveSetarStatusPago() {
        Pedido pedido = criarPedido();

        when(repository.porIdComItens(1L)).thenReturn(pedido);

        service.aprovaPagamentoPedido(1L);

        assertEquals(Status.PAGO, pedido.getStatus());
        verify(repository).atualizaStatus(Status.PAGO, pedido);
    }

    @Test
    void aprovaPagamentoPedido_quandoPedidoNaoExiste_deveLancarEntityNotFoundException() {
        when(repository.porIdComItens(99L)).thenReturn(null);

        assertThrows(EntityNotFoundException.class, () -> service.aprovaPagamentoPedido(99L));
        verify(repository, never()).atualizaStatus(any(), any());
    }
}
