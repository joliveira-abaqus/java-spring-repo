package br.com.alurafood.pagamentos;

import br.com.alurafood.pagamentos.http.PedidoClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PagamentosApplicationTests {

	@MockitoBean
	private PedidoClient pedidoClient;

	@Test
	void contextLoads() {
	}

}
