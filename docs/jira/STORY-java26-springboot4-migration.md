# Migração do Repositório para Java 26 e Spring Boot 4

## Informações Gerais

| Campo              | Valor                                    |
| :----------------- | :--------------------------------------- |
| **Tipo**           | História Técnica / Infraestrutura        |
| **Projeto**        | AluraFood - java-spring-repo             |
| **Prioridade**     | Alta                                     |
| **Estimativa**     | A definir pelo time                      |
| **Sprint**         | A definir                                |

---

## Descrição

**Como** desenvolvedor do time AluraFood,
**Eu quero** migrar o repositório `java-spring-repo` de Java 25 + Spring Boot 3.5.0 para Java 26 + Spring Boot 4.0.x,
**Para que** o projeto se mantenha atualizado com as versões mais recentes do JDK e do framework, garantindo suporte a longo prazo, melhorias de desempenho e acesso às novas funcionalidades.

---

## Contexto Técnico Atual

### Versões Atuais
| Componente              | Versão Atual         | Versão Alvo          |
| :---------------------- | :------------------- | :------------------- |
| Java (JDK)              | 25                   | 26                   |
| Spring Boot             | 3.5.0                | 4.0.x                |
| Spring Cloud BOM        | 2025.0.1             | Compatível com SB 4  |
| Resilience4j            | 2.3.0                | Compatível com SB 4  |
| Lombok                  | 1.18.44              | Compatível com Java 26 |
| ModelMapper             | 3.2.0                | Verificar compatibilidade |
| Eclipse Temurin (Docker)| 25-jdk / 25-jre      | 26-jdk / 26-jre      |
| MySQL Connector         | Gerenciado pelo SB   | Verificar compatibilidade |
| Flyway                  | Gerenciado pelo SB   | Verificar compatibilidade |
| Testcontainers          | Gerenciado pelo SB   | Verificar compatibilidade |

### Módulos Impactados

| Módulo        | Descrição                                      | Porta | Dependências Específicas                                              |
| :------------ | :--------------------------------------------- | :---- | :-------------------------------------------------------------------- |
| **server**    | Eureka Discovery Server                        | 8081  | `spring-cloud-starter-netflix-eureka-server`, Actuator                |
| **gateway**   | Spring Cloud Gateway (WebFlux)                  | 8082  | `spring-cloud-starter-gateway-server-webflux`, Eureka Client, Actuator|
| **pagamentos**| Microsserviço de pagamentos                    | 8083  | JPA, Flyway, Eureka Client, OpenFeign, Resilience4j, ModelMapper, Lombok, Testcontainers |
| **pedidos**   | Microsserviço de pedidos                       | 8084  | JPA, Flyway, Eureka Client, ModelMapper, Lombok, Testcontainers      |

---

## Critérios de Aceite

- [ ] Todos os POMs dos módulos atualizados para Spring Boot 4.0.x e Java 26
- [ ] Propriedade `<java.version>` alterada de `25` para `26` em todos os POMs
- [ ] `spring-boot-starter-parent` atualizado para versão `4.0.x` em todos os POMs
- [ ] Spring Cloud BOM (`spring-cloud.version`) atualizado para versão compatível com Spring Boot 4
- [ ] Resilience4j atualizado para versão compatível com Spring Boot 4 (verificar se `resilience4j-spring-boot3` foi renomeado para `resilience4j-spring-boot4` ou equivalente)
- [ ] Resilience4j BOM atualizado no `dependencyManagement` do módulo `pagamentos`
- [ ] Todos os Dockerfiles atualizados de `eclipse-temurin:25-jdk` / `eclipse-temurin:25-jre` para `eclipse-temurin:26-jdk` / `eclipse-temurin:26-jre`
- [ ] Todas as APIs deprecadas do Spring Boot 3.x substituídas pelos equivalentes do Spring Boot 4
- [ ] Todos os arquivos `application.yml` revisados para propriedades de configuração renomeadas ou removidas no Spring Boot 4
- [ ] Compatibilidade do Eureka Server/Client verificada (ou migrado para alternativa, se necessário)
- [ ] Compatibilidade do Spring Cloud Gateway (WebFlux) verificada
- [ ] Compatibilidade do OpenFeign verificada
- [ ] Migrações Flyway verificadas com a versão empacotada no Spring Boot 4
- [ ] Compatibilidade do Lombok com Java 26 verificada
- [ ] Compatibilidade do ModelMapper com Spring Boot 4 verificada
- [ ] Todos os testes unitários e de integração existentes passando
- [ ] Aplicação inicia e executa com sucesso no ambiente Docker Compose
- [ ] `docker-compose.yml` revisado para garantir compatibilidade com as novas imagens

---

## Detalhes Técnicos / Notas de Implementação

### 1. Alterações nos POMs

Cada módulo possui seu próprio `pom.xml`. As seguintes alterações devem ser feitas em **todos os 4 módulos** (`server`, `gateway`, `pagamentos`, `pedidos`):

```xml
<!-- DE -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.0</version>
</parent>
<properties>
    <java.version>25</java.version>
    <spring-cloud.version>2025.0.1</spring-cloud.version>
</properties>

<!-- PARA -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.x</version> <!-- Substituir pela versão estável mais recente -->
</parent>
<properties>
    <java.version>26</java.version>
    <spring-cloud.version>XXXX.X.X</spring-cloud.version> <!-- Versão compatível com SB 4 -->
</properties>
```

#### Módulo `pagamentos` - Alterações Adicionais
```xml
<!-- Verificar se o artefato mudou de nome -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId> <!-- Pode mudar para resilience4j-spring-boot4 -->
    <version>X.X.X</version> <!-- Versão compatível com SB 4 -->
</dependency>

<!-- Atualizar BOM do Resilience4j -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-bom</artifactId>
    <version>X.X.X</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

### 2. Alterações nos Dockerfiles

Todos os 4 Dockerfiles seguem o mesmo padrão multi-stage. Atualizar em cada um:

```dockerfile
# DE
FROM eclipse-temurin:25-jdk AS build
...
FROM eclipse-temurin:25-jre

# PARA
FROM eclipse-temurin:26-jdk AS build
...
FROM eclipse-temurin:26-jre
```

**Arquivos afetados:**
- `server/Dockerfile`
- `gateway/Dockerfile`
- `pagamentos/Dockerfile`
- `pedidos/Dockerfile`

### 3. Revisão dos `application.yml`

Verificar as notas de lançamento do Spring Boot 4 para propriedades renomeadas ou removidas. Arquivos a revisar:

| Módulo        | Arquivo                                          | Pontos de Atenção                                                |
| :------------ | :----------------------------------------------- | :--------------------------------------------------------------- |
| **server**    | `server/src/main/resources/application.yml`      | Configurações do Eureka Server                                   |
| **gateway**   | `gateway/src/main/resources/application.yml`     | `spring.cloud.gateway.discovery.locator` - verificar namespace   |
| **pagamentos**| `pagamentos/src/main/resources/application.yml`  | Config do Resilience4j, Flyway, JPA                              |
| **pedidos**   | `pedidos/src/main/resources/application.yml`     | Flyway, JPA                                                      |

### 4. Breaking Changes Potenciais a Investigar

- **Spring Boot 4**: Revisar o guia de migração oficial (quando disponível) para mudanças na API, remoção de classes deprecadas e alterações de comportamento padrão
- **Spring Cloud Gateway**: Verificar se `spring-cloud-starter-gateway-server-webflux` foi renomeado ou reestruturado
- **Netflix Eureka**: Verificar continuidade do suporte no ecossistema Spring Cloud compatível com SB 4
- **OpenFeign**: Verificar se `spring-cloud-starter-openfeign` continua sendo o artefato correto
- **Resilience4j**: O starter `resilience4j-spring-boot3` provavelmente terá uma nova versão específica para SB 4
- **Lombok**: Verificar compatibilidade com Java 26 (novas features da linguagem podem impactar a geração de código)
- **Flyway**: Verificar compatibilidade da versão empacotada com MySQL 8.0 e as migrações existentes
- **Jakarta EE**: Verificar se houve atualização na versão das APIs Jakarta (Persistence, Validation, etc.)

### 5. Migrações Flyway Existentes

As seguintes migrações devem continuar funcionando após a atualização:

- `pagamentos/src/main/resources/db/migration/V1__criar_tabela_pagamentos.sql`
- `pedidos/src/main/resources/db/migration/V1__cria_tabela_pedidos.sql`
- `pedidos/src/main/resources/db/migration/V2__cria_tabela_item_pedido.sql`

### 6. Docker Compose

O arquivo `docker-compose.yml` utiliza MySQL 8.0, que deve continuar compatível. Verificar se as imagens `eclipse-temurin:26` estão disponíveis nos builds dos serviços.

---

## Estratégia de Testes

1. **Testes unitários**: Executar `mvn test` em cada módulo individualmente
2. **Testes de integração**: Verificar testes com Testcontainers (módulos `pagamentos` e `pedidos`)
3. **Teste end-to-end**: Subir o ambiente completo via `docker-compose up` e validar:
   - Eureka Server acessível em `http://localhost:8081`
   - Todos os serviços registrados no Eureka
   - Gateway roteando requisições corretamente
   - CRUD de pagamentos funcionando
   - CRUD de pedidos funcionando
   - Circuit Breaker do Resilience4j funcionando no fluxo pagamentos -> pedidos

---

## Riscos e Dependências

| Risco                                                    | Mitigação                                                   |
| :------------------------------------------------------- | :---------------------------------------------------------- |
| Spring Boot 4 ainda não lançado ou em versão RC          | Aguardar versão GA ou utilizar versão RC com ciência do time |
| Spring Cloud sem versão compatível com SB 4              | Verificar roadmap do Spring Cloud; considerar alternativas  |
| Resilience4j sem suporte a SB 4                          | Avaliar migração para Spring Retry ou outra solução         |
| Imagem `eclipse-temurin:26` não disponível               | Aguardar lançamento ou usar imagem alternativa (Azul Zulu)  |
| Lombok incompatível com Java 26                          | Considerar remoção gradual do Lombok usando records do Java |
| Quebras de API não documentadas                          | Manter branch de migração isolada para testes extensivos    |

---

## Definição de Pronto (Definition of Done)

- Todos os microsserviços compilam com sucesso em Java 26 e Spring Boot 4
- Todos os testes unitários e de integração passam
- A aplicação completa executa end-to-end no ambiente Docker Compose
- Nenhuma dependência com vulnerabilidades conhecidas introduzida
- Código revisado por pelo menos um membro do time
- Documentação (README, CHANGELOG) atualizada para refletir as novas versões
