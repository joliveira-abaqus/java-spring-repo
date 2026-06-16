# AluraFood — Microserviços com Java e Spring

Backend baseado em microserviços para a plataforma AluraFood, gerenciando pedidos e pagamentos com autenticação JWT, service discovery, API gateway e circuit breaker.

---

## Sumário

- [Arquitetura](#arquitetura)
- [Pré-requisitos](#pré-requisitos)
- [Quick Start](#quick-start)
  - [Via Docker Compose](#via-docker-compose)
  - [Execução Local](#execução-local)
- [Microserviços](#microserviços)
  - [auth](#auth--serviço-de-autenticação)
  - [gateway](#gateway--api-gateway)
  - [server](#server--eureka-discovery)
  - [pagamentos](#pagamentos--processamento-de-pagamentos)
  - [pedidos](#pedidos--gerenciamento-de-pedidos)
- [Variáveis de Ambiente e Configuração](#variáveis-de-ambiente-e-configuração)
- [Database](#database)
- [Testes](#testes)
- [Docker](#docker)
- [Contribuição](#contribuição)
- [Licença](#licença)

---

## Arquitetura

```
                         ┌────────────────────┐
                         │   Eureka Server    │
                         │     (:8081)        │
                         └────────┬───────────┘
                                  │ registra/descobre
          ┌───────────────────────┼───────────────────────┐
          │                       │                       │
          ▼                       ▼                       ▼
┌──────────────────┐   ┌──────────────────┐   ┌──────────────────┐
│   Auth Service   │   │ Pagamentos MS    │   │   Pedidos MS     │
│     (:8085)      │   │   (:8083/dyn)    │   │   (:8084/dyn)    │
│  MySQL auth DB   │   │  MySQL pag. DB   │   │  MySQL ped. DB   │
└──────────────────┘   └────────┬─────────┘   └──────────────────┘
          ▲                     │ OpenFeign            ▲
          │                     └─────────────────────►│
          │                                            │
          │         ┌──────────────────────┐           │
          └─────────┤   API Gateway        ├───────────┘
     valida JWT     │     (:8082)          │  injeta X-Gateway-Secret
                    └──────────┬───────────┘
                               │
                          Requisições
                           externas
```

**Fluxo de uma requisição autenticada:**

1. O cliente envia a requisição para o **Gateway** (`:8082`) com `Authorization: Bearer <token>`.
2. O Gateway valida o JWT usando o `jwt.secret` compartilhado.
3. Se válido, injeta os headers `X-Auth-User-Email`, `X-Auth-User-Role` e `X-Gateway-Secret`.
4. O microserviço downstream verifica o `X-Gateway-Secret` via `GatewayAuthFilter` e autentica o usuário no SecurityContext.
5. Em caso de falha no downstream, o **Circuit Breaker** (Resilience4j) aciona o fallback.

---

## Pré-requisitos

| Ferramenta | Versão Mínima | Observação |
|------------|---------------|------------|
| Java | 25 | Eclipse Temurin recomendado |
| Maven | 3.9+ | Wrapper (`mvnw`) incluído em cada módulo |
| Docker | 24+ | Para execução via containers |
| Docker Compose | 2.20+ | Plugin do Docker CLI |
| MySQL | 8.0 | Apenas para execução local (sem Docker) |

---

## Quick Start

### Via Docker Compose

```bash
# Clone o repositório
git clone https://github.com/joliveira-abaqus/java-spring-repo.git
cd java-spring-repo

# Suba todos os serviços
docker compose up --build -d
```

Serviços disponíveis:

| Serviço | URL |
|---------|-----|
| Eureka Dashboard | http://localhost:8081 |
| API Gateway | http://localhost:8082 |
| Pagamentos (direto) | http://localhost:8083 |
| Pedidos (direto) | http://localhost:8084 |
| Auth (direto) | http://localhost:8085 |

### Execução Local

Inicie os serviços na seguinte ordem (cada um em um terminal separado):

```bash
# 1. Eureka Server
cd server && ./mvnw spring-boot:run

# 2. Auth (requer MySQL na porta 3308 ou 3306 com database alurafood-auth)
cd auth && ./mvnw spring-boot:run

# 3. Gateway
cd gateway && ./mvnw spring-boot:run

# 4. Pagamentos (requer MySQL com database alurafood-pagamento)
cd pagamentos && ./mvnw spring-boot:run

# 5. Pedidos (requer MySQL com database alurafood-pedidos)
cd pedidos && ./mvnw spring-boot:run
```

> **Nota:** Em execução local, `pagamentos` e `pedidos` utilizam porta dinâmica atribuída pelo Spring (`server.port=0`). Consulte o Eureka Dashboard para descobrir a porta alocada.

---

## Microserviços

### auth — Serviço de Autenticação

| | |
|-|-|
| **Porta** | 8085 |
| **Spring Name** | `auth-ms` |
| **Database** | `alurafood-auth` (MySQL) |

Responsável pela identidade dos usuários e emissão de tokens JWT.

**Endpoints:**

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/auth/registro` | Registra novo usuário |
| POST | `/auth/login` | Autentica e retorna JWT |
| GET | `/auth/validar?token=` | Valida um token JWT |

**Componentes principais:**
- `AuthController` — endpoints REST de autenticação
- `AuthService` — lógica de registro e login (BCrypt + JWT)
- `JwtService` — geração e validação de tokens (jjwt 0.12.6)
- `SecurityConfig` — configuração do Spring Security (rotas públicas: `/auth/**`)
- `JwtAuthenticationFilter` — filtro de validação de token em requisições protegidas

**Modelo de dados:**
- Tabela `usuarios`: `id`, `nome`, `email` (unique), `senha` (BCrypt), `role`, `ativo`

---

### gateway — API Gateway

| | |
|-|-|
| **Porta** | 8082 |
| **Spring Name** | `gateway` |
| **Framework** | Spring Cloud Gateway (WebFlux) |

Ponto de entrada único para todas as requisições externas. Realiza validação de JWT e injeta headers de segurança para comunicação interna.

**Funcionalidades:**
- Roteamento dinâmico via Eureka Service Discovery (`lower-case-service-id: true`)
- Validação de JWT em rotas protegidas (`AuthFilter` — `GlobalFilter`)
- Injeção de headers trusted: `X-Auth-User-Email`, `X-Auth-User-Role`, `X-Gateway-Secret`
- Rotas públicas (sem autenticação): `/auth/registro`, `/auth/login`, `/auth/validar`, `/eureka/**`

**Componentes principais:**
- `AuthFilter` — filtro global que intercepta requisições, valida o Bearer token e propaga identidade
- `RouteValidator` — define padrões de rotas públicas via regex

---

### server — Eureka Discovery

| | |
|-|-|
| **Porta** | 8081 |
| **Spring Name** | `server` |

Eureka Server para registro e descoberta de serviços. Todos os microserviços se registram automaticamente ao iniciar.

**Configuração:**
- `register-with-eureka: false` — não se registra em si mesmo
- `fetch-registry: false` — não busca registry de outros servers
- Dashboard disponível em http://localhost:8081

---

### pagamentos — Processamento de Pagamentos

| | |
|-|-|
| **Porta** | 8083 (Docker) / dinâmica (local) |
| **Spring Name** | `pagamentos-ms` |
| **Database** | `alurafood-pagamento` (MySQL) |

Microserviço responsável pelo ciclo de vida de pagamentos: criação, atualização, confirmação e cancelamento.

**Endpoints:**

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/pagamentos` | Lista pagamentos (paginado) |
| GET | `/pagamentos/{id}` | Detalhes de um pagamento |
| POST | `/pagamentos` | Cria novo pagamento |
| PUT | `/pagamentos/{id}` | Atualiza pagamento |
| DELETE | `/pagamentos/{id}` | Remove pagamento |
| PATCH | `/pagamentos/{id}/confirmar` | Confirma pagamento + atualiza pedido |

**Status do Pagamento:** `CRIADO` → `CONFIRMADO` → (ou `CONFIRMADO_SEM_INTEGRACAO` / `CANCELADO`)

**Componentes principais:**
- `PagamentoController` — endpoints REST com Circuit Breaker na confirmação
- `PagamentoService` — regras de negócio e conversão DTO ↔ Entity (ModelMapper)
- `PedidoClient` — Feign Client para comunicação com `pedidos-ms` (PUT `/pedidos/{id}/pago`)
- `GatewayAuthFilter` — valida o header `X-Gateway-Secret` para aceitar requisições internas
- `SecurityConfig` — Spring Security com filtro de trusted header
- `FeignClientInterceptor` — propaga o `X-Gateway-Secret` para chamadas inter-serviço

**Resilience4j (Circuit Breaker):**
- Instance: `atualizaPedido`
- `slidingWindowSize: 3`
- `minimumNumberOfCalls: 2`
- `waitDurationInOpenState: 50s`
- Fallback: `pagamentoAutorizadoComIntegracaoPendente` — marca como `CONFIRMADO_SEM_INTEGRACAO`

---

### pedidos — Gerenciamento de Pedidos

| | |
|-|-|
| **Porta** | 8084 (Docker) / dinâmica (local) |
| **Spring Name** | `pedidos-ms` |
| **Database** | `alurafood-pedidos` (MySQL) |

Microserviço responsável por criar, listar e gerenciar o ciclo de vida dos pedidos.

**Endpoints:**

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/pedidos` | Lista todos os pedidos |
| GET | `/pedidos/{id}` | Detalhes de um pedido |
| GET | `/pedidos/porta` | Retorna a porta da instância (debug) |
| POST | `/pedidos` | Cria novo pedido |
| PUT | `/pedidos/{id}/status` | Atualiza status do pedido |
| PUT | `/pedidos/{id}/pago` | Marca pedido como pago (chamado pelo pagamentos-ms) |

**Status do Pedido:** `REALIZADO` → `PAGO` → `CONFIRMADO` → `PRONTO` → `SAIU_PARA_ENTREGA` → `ENTREGUE` (ou `CANCELADO` / `NAO_AUTORIZADO`)

**Componentes principais:**
- `PedidoController` — endpoints REST
- `PedidoService` — lógica de negócio e conversão DTO ↔ Entity (ModelMapper)
- `GatewayAuthFilter` — valida `X-Gateway-Secret`
- `SecurityConfig` — configuração de segurança

**Modelo de dados:**
- Tabela `pedidos`: `id`, `data_hora`, `status`
- Tabela `item_do_pedido`: `id`, `descricao`, `quantidade`, `pedido_id` (FK → pedidos)

---

## Variáveis de Ambiente e Configuração

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `MYSQL_ROOT_PASSWORD` | Senha root dos containers MySQL | `changeme` |
| `JWT_SECRET` | Chave Base64 para assinatura JWT (min. 256 bits) | Chave de desenvolvimento |
| `GATEWAY_SECRET` | Segredo compartilhado para trusted header `X-Gateway-Secret` | `minha-chave-secreta-gateway-2025` |
| `SPRING_DATASOURCE_URL` | URL JDBC do banco de dados | Varia por serviço |
| `SPRING_DATASOURCE_USERNAME` | Usuário do banco | `root` |
| `SPRING_DATASOURCE_PASSWORD` | Senha do banco | (vazio em local) |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | URL do Eureka Server | `http://localhost:8081/eureka` |
| `SERVER_PORT` | Porta do serviço | Varia por serviço |

> ⚠️ **Produção:** Substitua `JWT_SECRET` e `GATEWAY_SECRET` por valores seguros e não commite segredos no repositório.

---

## Database

Cada microserviço possui seu próprio banco de dados MySQL isolado (**Database per Service**):

| Serviço | Database | Porta (Docker) |
|---------|----------|----------------|
| auth | `alurafood-auth` | 3308 |
| pagamentos | `alurafood-pagamento` | 3306 |
| pedidos | `alurafood-pedidos` | 3307 |

### Flyway Migrations

As migrações são gerenciadas pelo **Flyway** e executadas automaticamente ao iniciar cada serviço:

```
auth/src/main/resources/db/migration/
  └── V1__criar_tabela_usuarios.sql

pagamentos/src/main/resources/db/migration/
  ├── V1__criar_tabela_pagamentos.sql
  └── V2__insere_dados_iniciais.sql

pedidos/src/main/resources/db/migration/
  ├── V1__cria_tabela_pedidos.sql
  ├── V2__cria_tabela_item_pedido.sql
  └── V3__insere_dados_iniciais.sql
```

---

## Testes

Os testes utilizam **H2 in-memory** (perfil `test`), sem necessidade de MySQL rodando.

```bash
# Testes do módulo pagamentos
cd pagamentos && ./mvnw test

# Testes do módulo pedidos
cd pedidos && ./mvnw test

# Rodar todos os testes de todos os módulos
for mod in pagamentos pedidos; do (cd $mod && ./mvnw test -B); done
```

**Stack de testes:**
- JUnit 5 + Spring Boot Test
- H2 Database (perfil `test`)
- Testcontainers (MySQL) disponível para testes de integração avançados
- Spring Security Test

---

## Docker

### Build e Execução

```bash
# Subir todos os serviços em background
docker compose up --build -d

# Ver logs
docker compose logs -f

# Parar todos os serviços
docker compose down

# Parar e remover volumes (limpar dados)
docker compose down -v
```

### Multi-stage Build

Cada serviço utiliza **multi-stage Docker build** para otimização:

1. **Stage build:** `eclipse-temurin:25-jdk` — compila o projeto com Maven
2. **Stage runtime:** `eclipse-temurin:25-jre` — imagem mínima para execução

Exemplo (`auth/Dockerfile`):

```dockerfile
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && sh mvnw dependency:go-offline -B
COPY src src
RUN sh mvnw package -DskipTests -B

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## Contribuição

1. Faça um fork do repositório
2. Crie uma branch com o prefixo `feature/`: `git checkout -b feature/minha-feature`
3. Faça commit das suas alterações (mensagens em português): `git commit -m "feat: adicionar nova funcionalidade"`
4. Faça push para a branch: `git push origin feature/minha-feature`
5. Abra um Pull Request com descrição em português

### Convenções

- **Branches:** prefixo `feature/` com kebab-case (ex: `feature/adicionar-endpoint-pagamento`)
- **Commits:** mensagens em português (BR)
- **Código:** comentários e Javadoc em português
- **PRs:** título e descrição em português

---

## Licença

Este projeto é disponibilizado para fins educacionais.

---

## Atribuição

_Originally written and maintained by contributors and [Devin](https://app.devin.ai), with updates from the core team._
