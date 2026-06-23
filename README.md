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

O projeto é composto por 4 microsserviços:

| Serviço | Porta | Descrição |
|---------|-------|-----------|
| **server** | 9001 | Eureka Service Discovery |
| **gateway** | 9002 | API Gateway (Spring Cloud Gateway) |
| **pagamentos** | 9003 (Docker) / dinâmica (local) | Microsserviço de pagamentos |
| **pedidos** | 9004 (Docker) / dinâmica (local) | Microsserviço de pedidos |

## Executar com Docker Compose

```bash
docker-compose up --build
```

Isso irá iniciar todos os serviços, incluindo duas instâncias MySQL (uma para pagamentos e outra para pedidos), o Eureka Server, o API Gateway e os microsserviços.

- Eureka Dashboard: http://localhost:9001
- API Gateway: http://localhost:9002
- Pagamentos (direto): http://localhost:9003
- Pedidos (direto): http://localhost:9004

## Executar localmente

Iniciar os serviços na seguinte ordem:

```bash
# 1. Eureka Server
cd server && ./mvnw spring-boot:run

# 2. Gateway
cd gateway && ./mvnw spring-boot:run

# 3. Pagamentos (requer MySQL local na porta 3306)
cd pagamentos && ./mvnw spring-boot:run

# 4. Pedidos (requer MySQL local na porta 3306)
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
