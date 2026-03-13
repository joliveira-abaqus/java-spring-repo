package br.com.alurafood.pedidos.service;

import br.com.alurafood.pedidos.dto.ItemDoPedidoDto;
import br.com.alurafood.pedidos.dto.PedidoDto;
import br.com.alurafood.pedidos.dto.StatusDto;
import br.com.alurafood.pedidos.model.ItemDoPedido;
import br.com.alurafood.pedidos.model.Pedido;
import br.com.alurafood.pedidos.model.Status;
import br.com.alurafood.pedidos.repository.PedidoRepository;
import jakarta.persistence.EntityNotFoundException;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository repository;

    @Mock
    private ModelMapper modelMapper;

    private PedidoService service;

    private Pedido pedido;
    private PedidoDto pedidoDto;

    @BeforeEach
    void setUp() throws Exception {
        // PedidoService uses @RequiredArgsConstructor for modelMapper (final)
        // and @Autowired field injection for repository
        service = new PedidoService(modelMapper);

        // Inject repository via reflection since it uses @Autowired field injection
        Field repoField = PedidoService.class.getDeclaredField("repository");
        repoField.setAccessible(true);
        repoField.set(service, repository);

        pedido = new Pedido();
        pedido.setId(1L);
        pedido.setDataHora(LocalDateTime.now());
        pedido.setStatus(Status.REALIZADO);
        pedido.setItens(new ArrayList<>());

        ItemDoPedido item = new ItemDoPedido();
        item.setId(1L);
        item.setQuantidade(2);
        item.setDescricao("Pizza");
        item.setPedido(pedido);
        pedido.getItens().add(item);

        pedidoDto = new PedidoDto();
        pedidoDto.setId(1L);
        pedidoDto.setDataHora(LocalDateTime.now());
        pedidoDto.setStatus(Status.REALIZADO);

        ItemDoPedidoDto itemDto = new ItemDoPedidoDto();
        itemDto.setId(1L);
        itemDto.setQuantidade(2);
        itemDto.setDescricao("Pizza");
        pedidoDto.setItens(new ArrayList<>(List.of(itemDto)));
    }

    @Test
    void obterTodos_deveRetornarListaDePedidos() {
        when(repository.findAll()).thenReturn(List.of(pedido));
        when(modelMapper.map(any(Pedido.class), eq(PedidoDto.class))).thenReturn(pedidoDto);

        List<PedidoDto> result = service.obterTodos();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository).findAll();
    }

    @Test
    void obterPorId_deveRetornarPedidoQuandoExiste() {
        when(repository.findById(1L)).thenReturn(Optional.of(pedido));
        when(modelMapper.map(pedido, PedidoDto.class)).thenReturn(pedidoDto);

        PedidoDto result = service.obterPorId(1L);

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
    void criarPedido_deveCriarComStatusRealizadoEDataHora() {
        Pedido novoPedido = new Pedido();
        novoPedido.setItens(new ArrayList<>());
        ItemDoPedido novoItem = new ItemDoPedido();
        novoItem.setQuantidade(2);
        novoItem.setDescricao("Pizza");
        novoPedido.getItens().add(novoItem);

        when(modelMapper.map(pedidoDto, Pedido.class)).thenReturn(novoPedido);
        when(repository.save(novoPedido)).thenReturn(novoPedido);
        when(modelMapper.map(novoPedido, PedidoDto.class)).thenReturn(pedidoDto);

        PedidoDto result = service.criarPedido(pedidoDto);

        assertNotNull(result);
        assertEquals(Status.REALIZADO, novoPedido.getStatus());
        assertNotNull(novoPedido.getDataHora());
        verify(repository).save(novoPedido);
    }

    @Test
    void atualizaStatus_deveAtualizarStatusDoPedido() {
        StatusDto statusDto = new StatusDto();
        statusDto.setStatus(Status.CONFIRMADO);

        when(repository.porIdComItens(1L)).thenReturn(pedido);
        when(modelMapper.map(pedido, PedidoDto.class)).thenReturn(pedidoDto);

        PedidoDto result = service.atualizaStatus(1L, statusDto);

        assertNotNull(result);
        assertEquals(Status.CONFIRMADO, pedido.getStatus());
        verify(repository).atualizaStatus(Status.CONFIRMADO, pedido);
    }

    @Test
    void atualizaStatus_deveLancarExcecaoQuandoNaoExiste() {
        StatusDto statusDto = new StatusDto();
        statusDto.setStatus(Status.CONFIRMADO);

        when(repository.porIdComItens(99L)).thenReturn(null);

        assertThrows(EntityNotFoundException.class, () -> service.atualizaStatus(99L, statusDto));
    }

    @Test
    void aprovaPagamentoPedido_deveAtualizarStatusParaPago() {
        when(repository.porIdComItens(1L)).thenReturn(pedido);

        service.aprovaPagamentoPedido(1L);

        assertEquals(Status.PAGO, pedido.getStatus());
        verify(repository).atualizaStatus(Status.PAGO, pedido);
    }

    @Test
    void aprovaPagamentoPedido_deveLancarExcecaoQuandoNaoExiste() {
        when(repository.porIdComItens(99L)).thenReturn(null);

        assertThrows(EntityNotFoundException.class, () -> service.aprovaPagamentoPedido(99L));
    }
}
