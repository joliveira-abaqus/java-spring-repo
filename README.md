# Microsserviços com Java e Spring

Projeto de microsserviços com Java e Spring, atualizado para as versões mais recentes.

## Versões

| Tecnologia | Versão |
|------------|--------|
| Java | 25 |
| Spring Boot | 4.1.0 |
| Spring Cloud | 2025.1.2 |
| Resilience4j | 2.3.0 |
| MySQL | 8.0 |
| Flyway | (gerido pelo Spring Boot) |

## Arquitetura

O projeto é composto por 5 microsserviços:

| Serviço | Porta | Descrição |
|---------|-------|-----------|
| **server** | 8081 | Eureka Service Discovery |
| **gateway** | 8082 | API Gateway (Spring Cloud Gateway) |
| **auth** | 8085 | Autenticação e geração de JWT |
| **pagamentos** | 8083 (Docker) / dinâmica (local) | Microsserviço de pagamentos |
| **pedidos** | 8084 (Docker) / dinâmica (local) | Microsserviço de pedidos |

## Pré-requisitos

- **Java 25** (Eclipse Temurin recomendado)
- **Docker** e **Docker Compose** (para execução com containers)
- **MySQL 8.0** (para execução local sem Docker)

## Configuração local

Crie um arquivo `.env` na raiz do projeto (ele já está no `.gitignore`):

```bash
MYSQL_ROOT_PASSWORD=changeme
GATEWAY_SECRET=uma-chave-segura-para-dev
JWT_SECRET=YWx1cmFmb29kLXNlY3JldC1rZXktand0LXNlY3VyaXR5LTI1Ni1iaXRzLW1pbmltdW0=
```

> Nunca commitar o `.env`. Para produção, use um gerenciador de segredos.

## Executar com Docker Compose

```bash
docker compose up --build
```

Isso inicia todos os serviços:

- 3 instâncias MySQL (auth, pagamentos e pedidos)
- Eureka Server
- API Gateway
- Auth, Pagamentos e Pedidos

- Eureka Dashboard: http://localhost:8081
- API Gateway: http://localhost:8082
- Auth (direto): http://localhost:8085
- Pagamentos (direto): http://localhost:8083
- Pedidos (direto): http://localhost:8084

Para parar e remover os containers:

```bash
docker compose down
```

Para limpar também os volumes (remove os dados dos bancos):

```bash
docker compose down -v
```

## Executar localmente

Iniciar os serviços na seguinte ordem:

```bash
# 1. Eureka Server
cd server && ./mvnw spring-boot:run

# 2. Gateway
cd gateway && ./mvnw spring-boot:run

# 3. Auth (requer MySQL local na porta 3308)
cd auth && ./mvnw spring-boot:run

# 4. Pagamentos (requer MySQL local na porta 3306)
cd pagamentos && ./mvnw spring-boot:run

# 5. Pedidos (requer MySQL local na porta 3307)
cd pedidos && ./mvnw spring-boot:run
```

As URLs de conexão com MySQL seguem o padrão:

```
jdbc:mysql://localhost:<porta>/<database>?createDatabaseIfNotExist=true
```

| Serviço | Porta MySQL | Database |
|---------|-------------|----------|
| auth | 3308 | alurafood-auth |
| pagamentos | 3306 | alurafood-pagamento |
| pedidos | 3307 | alurafood-pedidos |

## Testes manuais

Veja o arquivo [`testes-manuais.md`](testes-manuais.md) para uma sequência de chamadas HTTP para validar a autenticação, o gateway e os microsserviços.

## Executar testes

```bash
# Auth
cd auth && ./mvnw test

# Pagamentos
cd pagamentos && ./mvnw test

# Pedidos
cd pedidos && ./mvnw test
```

Os testes de integração utilizam H2 em memória (perfil `test`), sem necessidade de MySQL.

## Monitoramento com Datadog

O projeto está instrumentado para enviar **traces (APM)** e **métricas** para um
**Datadog Agent** em execução na máquina do desenvolvedor (host). Nenhum segredo
é commitado no repositório — a `DD_API_KEY` deve ser configurada apenas no Agent.

### Duas camadas de observabilidade

1. **APM / tracing (dd-java-agent)** — cada `Dockerfile` baixa o `dd-java-agent.jar`
   e o `ENTRYPOINT` inclui `-javaagent:/app/dd-java-agent.jar`, ativando o tracing
   automático de requisições HTTP, JDBC, Feign, etc.
2. **Métricas (Micrometer + DogStatsD)** — cada serviço inclui a dependência
   `io.micrometer:micrometer-registry-statsd` e exporta métricas para o DogStatsD
   do Agent (porta UDP `8125`), no formato `datadog`.

### Variáveis de ambiente

Definidas por serviço no `docker-compose.yml`:

| Variável | Valor | Descrição |
|----------|-------|-----------|
| `DD_SERVICE` | ex.: `pagamentos-ms` | Nome do serviço no Datadog |
| `DD_ENV` | `dev` | Ambiente |
| `DD_VERSION` | `0.0.1-SNAPSHOT` | Versão do serviço |
| `DD_AGENT_HOST` | `host.docker.internal` | Host onde o Agent está rodando |
| `DD_TRACE_ENABLED` | `true` | Habilita o tracing |
| `DD_LOGS_INJECTION` | `true` | Injeta `trace_id`/`span_id` nos logs |

No Linux, cada serviço define `extra_hosts: ["host.docker.internal:host-gateway"]`
para que os containers alcancem o Agent no host.

O DogStatsD usa o mesmo `DD_AGENT_HOST` (porta `8125`). Sem Docker, o padrão é
`localhost:8125`.

### Configurar o Datadog Agent no host

A **API key nunca deve ser commitada**. Configure-a apenas no Agent:

```bash
docker run -d --name datadog-agent \
  -e DD_API_KEY=<SUA_API_KEY> \
  -e DD_SITE=datadoghq.com \
  -e DD_APM_ENABLED=true \
  -e DD_APM_NON_LOCAL_TRAFFIC=true \
  -e DD_DOGSTATSD_NON_LOCAL_TRAFFIC=true \
  -p 8126:8126/tcp \
  -p 8125:8125/udp \
  -v /var/run/docker.sock:/var/run/docker.sock:ro \
  -v /proc/:/host/proc/:ro \
  -v /sys/fs/cgroup/:/host/sys/fs/cgroup:ro \
  gcr.io/datadoghq/agent:latest
```

> Alternativamente, se o Agent já estiver instalado nativamente no host, habilite
> APM (`apm_config.enabled: true`, porta `8126`) e DogStatsD com tráfego não-local
> (`dogstatsd_non_local_traffic: true`, porta `8125`) no `datadog.yaml`.

### Executar e visualizar

```bash
# 1. Inicie o Datadog Agent (com DD_API_KEY) conforme acima
# 2. Suba a stack
docker compose up --build
```

- **Traces (APM):** Datadog → *APM → Traces*, filtrando por `env:dev` e pelos
  serviços `server`, `gateway`, `auth-ms`, `pagamentos-ms`, `pedidos-ms`.
- **Métricas:** Datadog → *Metrics → Explorer*, buscando por métricas
  `jvm.*`, `system.*`, `http.server.requests.*` com a tag `service`.
- Localmente, os endpoints do Actuator continuam disponíveis, ex.:
  `http://localhost:8082/actuator/metrics`.

## Sobre o projeto

O projeto trabalhado no curso é o Alura Food, onde a ideia central é que o mesmo era um monolito e estamos iniciando a decomposição em microsserviços. Começamos implementando a API e projeto do microsserviço de pagamento, tendo um banco de dados próprio [MySQL](https://www.mysql.com).

Além disso, fazemos a implementação do Service Discovery utilizando o [Eureka](https://spring.io/projects/spring-cloud-netflix), solução desenvolvida pela Netflix e que faz parte do [Spring Cloud](https://spring.io/projects/spring-cloud). Incluímos também à arquitetura um [API Gateway](https://spring.io/projects/spring-cloud-gateway), que vai atuar como ponto central para as nossas requisições. É feita a inclusão de um novo microsserviço, que é o de pedidos, onde praticamos a comunicação síncrona e o balanceamento de carga, quando há mais de uma instância do projeto em execução.

Para fechar, tratamos os conceitos de circuit breaker e fallback, utilizando o [Resilience4J](https://resilience4j.readme.io/docs/getting-started-3) e promovendo uma alternativa quando um dos serviços está inoperante.
