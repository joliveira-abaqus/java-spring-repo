# Microsserviços com Java e Spring


Projeto de microsserviços com Java e Spring, atualizado para as versões mais recentes.

## Versões

| Tecnologia | Versão |
|------------|--------|
| Java | 25 |
| Spring Boot | 3.5.0 |
| Spring Cloud | 2025.0.1 |
| Resilience4j | 2.3.0 |
| MySQL | 8.0 |
| Flyway | (gerido pelo Spring Boot) |

## Pré-requisitos

- **Java 25** (Eclipse Temurin recomendado)
- **Docker** e **Docker Compose** (para execução com containers)
- **MySQL 8.0** (para execução local sem Docker)

## Arquitetura

O projeto é composto por 5 microsserviços:

| Serviço | Porta | Descrição |
|---------|-------|-----------|
| **server** | 8081 | Eureka Service Discovery |
| **gateway** | 8082 | API Gateway (Spring Cloud Gateway) |
| **auth** | 8085 | Microsserviço de autenticação (JWT) |
| **pagamentos** | 8083 (Docker) / dinâmica (local) | Microsserviço de pagamentos |
| **pedidos** | 8084 (Docker) / dinâmica (local) | Microsserviço de pedidos |

Cada microsserviço de negócio (`pagamentos`, `pedidos`, `auth`) possui seu próprio banco de dados MySQL, seguindo o padrão *database per service*.

## Autenticação

A autenticação é baseada em **JWT (JSON Web Token)**. O fluxo funciona da seguinte forma:

1. O cliente se registra (`POST /auth/registro`) ou faz login (`POST /auth/login`) no microsserviço **auth** e recebe um token JWT.
2. Nas requisições seguintes, o cliente envia o token no header `Authorization: Bearer <token>`.
3. O **gateway** valida o token, extrai as informações do usuário e as propaga para os microsserviços internos via headers (`X-Auth-User-Email`, `X-Auth-User-Role`, `X-Gateway-Secret`).
4. Os microsserviços internos aceitam apenas requisições que contenham o `X-Gateway-Secret` válido, garantindo que só o gateway consiga acessá-los diretamente.

As rotas públicas (não exigem autenticação) são: `/auth/registro`, `/auth/login`, `/auth/validar` e `/eureka/**`.

## Variáveis de ambiente

Antes de executar o projeto, configure as seguintes variáveis de ambiente:

| Variável | Obrigatória | Descrição |
|----------|-------------|-----------|
| `GATEWAY_SECRET` | Sim | Segredo compartilhado entre o gateway e os microsserviços internos |
| `JWT_SECRET` | Não (possui padrão) | Chave (Base64) usada para assinar/validar os tokens JWT |
| `MYSQL_ROOT_PASSWORD` | Não (padrão `changeme`) | Senha root das instâncias MySQL |

> **Importante:** em produção, defina sempre `GATEWAY_SECRET` e `JWT_SECRET` com valores próprios e seguros. Não utilize os valores padrão de desenvolvimento.

## Executar com Docker Compose

Defina primeiro as variáveis de ambiente obrigatórias (veja a seção acima) e depois execute:

```bash
export GATEWAY_SECRET="seu-segredo-do-gateway"
docker-compose up --build
```

Isso irá iniciar todos os serviços, incluindo três instâncias MySQL (uma para pagamentos, uma para pedidos e uma para auth), o Eureka Server, o API Gateway e os microsserviços.

- Eureka Dashboard: http://localhost:8081
- API Gateway: http://localhost:8082
- Auth (direto): http://localhost:8085
- Pagamentos (direto): http://localhost:8083
- Pedidos (direto): http://localhost:8084

## Executar localmente

Iniciar os serviços na seguinte ordem:

```bash
# 1. Eureka Server
cd server && ./mvnw spring-boot:run

# 2. Gateway
cd gateway && ./mvnw spring-boot:run

# 3. Auth (requer MySQL local)
cd auth && ./mvnw spring-boot:run

# 4. Pagamentos (requer MySQL local na porta 3306)
cd pagamentos && ./mvnw spring-boot:run

# 5. Pedidos (requer MySQL local na porta 3306)
cd pedidos && ./mvnw spring-boot:run
```

## Executar testes

```bash
# Pagamentos (22 testes: unitários + integração)
cd pagamentos && ./mvnw test

# Pedidos (19 testes: unitários + integração)
cd pedidos && ./mvnw test
```

Os testes de integração utilizam H2 em memória (perfil `test`), sem necessidade de MySQL.

## Sobre o projeto

<p>  O projeto trabalhado no curso é o Alura Food, onde a ideia central é que o mesmo era um monolito e estamos iniciando a decomposição em microsserviços. Começamos implementando a API e projeto do microsserviço de pagamento, tendo um banco de dados próprio [MySQL](https://www.mysql.com).
</p>

<p>  Além disso, fazemos a implementação do Service Discovery utilizando o [Eureka](https://spring.io/projects/spring-cloud-netflix),   solução desenvolvida pela Netflix e que faz parte do [Spring Cloud](https://spring.io/projects/spring-cloud). Incluímos também à arquitetura um [API Gateway](https://spring.io/projects/spring-cloud-gateway), que vai atuar como ponto central para as nossas requisições. É feita a inclusão de um novo microsserviço, que é o de pedidos, onde praticamos a comunicação síncrona e o balanceamento de carga, quando há mais de uma instância do projeto em execução.</p>

<p>  Para fechar, tratamos os conceitos de circuit breaker e fallback, utilizando o [Resilience4J](https://resilience4j.readme.io/docs/getting-started-3) e promovendo uma alternativa quando um dos serviços está inoperante.</p>
