package br.com.alurafood.gateway.config;

import br.com.alurafood.gateway.util.JwtTestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthFilterTest {

    @InjectMocks
    private AuthFilter authFilter;

    @Mock
    private RouteValidator routeValidator;

    @Mock
    private GatewayFilterChain chain;

    private static final String GATEWAY_SECRET = "test-gateway-secret";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authFilter, "secret", JwtTestUtil.SECRET);
        ReflectionTestUtils.setField(authFilter, "gatewaySecret", GATEWAY_SECRET);
    }

    @Nested
    @DisplayName("validateSecret")
    class ValidateSecret {

        @Test
        @DisplayName("deve lancar excecao quando gateway.secret e nulo")
        void deveLancarExcecaoQuandoGatewaySecretNulo() {
            ReflectionTestUtils.setField(authFilter, "gatewaySecret", null);
            assertThatThrownBy(() -> authFilter.validateSecret())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("gateway.secret");
        }

        @Test
        @DisplayName("deve lancar excecao quando gateway.secret e vazio")
        void deveLancarExcecaoQuandoGatewaySecretVazio() {
            ReflectionTestUtils.setField(authFilter, "gatewaySecret", "   ");
            assertThatThrownBy(() -> authFilter.validateSecret())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("gateway.secret");
        }

        @Test
        @DisplayName("nao deve lancar excecao quando gateway.secret e valido")
        void naoDeveLancarExcecaoQuandoGatewaySecretValido() {
            authFilter.validateSecret();
        }
    }

    @Nested
    @DisplayName("Rotas publicas")
    class RotasPublicas {

        @Test
        @DisplayName("deve permitir acesso a rotas publicas sem token")
        void devePermitirAcessoARotasPublicasSemToken() {
            var request = MockServerHttpRequest.get("/auth/login").build();
            var exchange = MockServerWebExchange.from(request);

            routeValidator.isRotaProtegida = req -> false;
            when(chain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(authFilter.filter(exchange, chain))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Requisicoes sem Authorization header")
    class SemAuthorizationHeader {

        @Test
        @DisplayName("deve retornar 401 quando header Authorization ausente")
        void deveRetornar401QuandoHeaderAusente() {
            var request = MockServerHttpRequest.get("/pedidos").build();
            var exchange = MockServerWebExchange.from(request);

            routeValidator.isRotaProtegida = req -> true;

            StepVerifier.create(authFilter.filter(exchange, chain))
                    .verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("Requisicoes com Authorization invalido")
    class ComAuthorizationInvalido {

        @Test
        @DisplayName("deve retornar 401 quando token nao comeca com Bearer")
        void deveRetornar401QuandoTokenSemBearer() {
            var request = MockServerHttpRequest.get("/pedidos")
                    .header(HttpHeaders.AUTHORIZATION, "Basic abc123")
                    .build();
            var exchange = MockServerWebExchange.from(request);

            routeValidator.isRotaProtegida = req -> true;

            StepVerifier.create(authFilter.filter(exchange, chain))
                    .verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("deve retornar 401 quando token e expirado")
        void deveRetornar401QuandoTokenExpirado() {
            String tokenExpirado = JwtTestUtil.gerarTokenExpirado("user@test.com", "USER");
            var request = MockServerHttpRequest.get("/pedidos")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenExpirado)
                    .build();
            var exchange = MockServerWebExchange.from(request);

            routeValidator.isRotaProtegida = req -> true;

            StepVerifier.create(authFilter.filter(exchange, chain))
                    .verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("deve retornar 401 quando token tem assinatura invalida")
        void deveRetornar401QuandoAssinaturaInvalida() {
            String tokenInvalido = JwtTestUtil.gerarTokenComChaveInvalida("user@test.com", "USER");
            var request = MockServerHttpRequest.get("/pedidos")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenInvalido)
                    .build();
            var exchange = MockServerWebExchange.from(request);

            routeValidator.isRotaProtegida = req -> true;

            StepVerifier.create(authFilter.filter(exchange, chain))
                    .verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("deve retornar 401 quando token e malformado")
        void deveRetornar401QuandoTokenMalformado() {
            var request = MockServerHttpRequest.get("/pedidos")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido-abc")
                    .build();
            var exchange = MockServerWebExchange.from(request);

            routeValidator.isRotaProtegida = req -> true;

            StepVerifier.create(authFilter.filter(exchange, chain))
                    .verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("Requisicoes com token valido")
    class ComTokenValido {

        @Test
        @DisplayName("deve propagar headers quando token e valido")
        void devePropagarHeadersQuandoTokenValido() {
            String token = JwtTestUtil.gerarToken("admin@test.com", "ADMIN");
            var request = MockServerHttpRequest.get("/pedidos")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();
            var exchange = MockServerWebExchange.from(request);

            routeValidator.isRotaProtegida = req -> true;
            when(chain.filter(any())).thenAnswer(invocation -> {
                var ex = invocation.getArgument(0, org.springframework.web.server.ServerWebExchange.class);
                assertThat(ex.getRequest().getHeaders().getFirst("X-Auth-User-Email"))
                        .isEqualTo("admin@test.com");
                assertThat(ex.getRequest().getHeaders().getFirst("X-Auth-User-Role"))
                        .isEqualTo("ADMIN");
                assertThat(ex.getRequest().getHeaders().getFirst("X-Gateway-Secret"))
                        .isEqualTo(GATEWAY_SECRET);
                return Mono.empty();
            });

            StepVerifier.create(authFilter.filter(exchange, chain))
                    .verifyComplete();
        }

        @Test
        @DisplayName("deve propagar role USER corretamente")
        void devePropagarRoleUser() {
            String token = JwtTestUtil.gerarToken("user@test.com", "USER");
            var request = MockServerHttpRequest.get("/pagamentos")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .build();
            var exchange = MockServerWebExchange.from(request);

            routeValidator.isRotaProtegida = req -> true;
            when(chain.filter(any())).thenAnswer(invocation -> {
                var ex = invocation.getArgument(0, org.springframework.web.server.ServerWebExchange.class);
                assertThat(ex.getRequest().getHeaders().getFirst("X-Auth-User-Email"))
                        .isEqualTo("user@test.com");
                assertThat(ex.getRequest().getHeaders().getFirst("X-Auth-User-Role"))
                        .isEqualTo("USER");
                return Mono.empty();
            });

            StepVerifier.create(authFilter.filter(exchange, chain))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Ordem do filtro")
    class OrdemDoFiltro {

        @Test
        @DisplayName("deve ter prioridade alta (order = -1)")
        void deveRetornarOrdemMenosUm() {
            assertThat(authFilter.getOrder()).isEqualTo(-1);
        }
    }
}
