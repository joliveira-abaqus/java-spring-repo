# Migracaoo do Repositorio para Java 26 e Spring Boot 4

## Informacoes Gerais

| Campo              | Valor                                    |
| :----------------- | :--------------------------------------- |
| **Tipo**           | Historia Tecnica / Infraestrutura        |
| **Projeto**        | AluraFood - java-spring-repo             |
| **Prioridade**     | Alta                                     |
| **Estimativa**     | A definir pelo time                      |
| **Sprint**         | A definir                                |

---

## Descricao

**Como** desenvolvedor do time AluraFood,
**Eu quero** migrar o repositorio `java-spring-repo` de Java 25 + Spring Boot 3.5.0 para Java 26 + Spring Boot 4.0.x,
**Para que** o projeto se mantenha atualizado com as versoes mais recentes do JDK e do framework, garantindo suporte a longo prazo, melhorias de desempenho e acesso as novas funcionalidades.

---

## Contexto Tecnico Atual

### Versoes Atuais
| Componente              | Versao Atual         | Versao Alvo          |
| :---------------------- | :------------------- | :------------------- |
| Java (JDK)              | 25                   | 26                   |
| Spring Boot             | 3.5.0                | 4.0.x                |
| Spring Cloud BOM        | 2025.0.1             | Compativel com SB 4  |
| Resilience4j            | 2.3.0                | Compativel com SB 4  |
| Lombok                  | 1.18.44              | Compativel com Java 26 |
| ModelMapper             | 3.2.0                | Verificar compatibilidade |
| Eclipse Temurin (Docker)| 25-jdk / 25-jre      | 26-jdk / 26-jre      |
| MySQL Connector         | Gerenciado pelo SB   | Verificar compatibilidade |
| Flyway                  | Gerenciado pelo SB   | Verificar compatibilidade |
| Testcontainers          | Gerenciado pelo SB   | Verificar compatibilidade |

### Modulos Impactados

| Modulo        | Descricao                                      | Porta | Dependencias Especificas                                              |
| :------------ | :--------------------------------------------- | :---- | :-------------------------------------------------------------------- |
| **server**    | Eureka Discovery Server                        | 8081  | `spring-cloud-starter-netflix-eureka-server`, Actuator                |
| **gateway**   | Spring Cloud Gateway (WebFlux)                  | 8082  | `spring-cloud-starter-gateway-server-webflux`, Eureka Client, Actuator|
| **pagamentos**| Microsservico de pagamentos                    | 8083  | JPA, Flyway, Eureka Client, OpenFeign, Resilience4j, ModelMapper, Lombok, Testcontainers |
| **pedidos**   | Microsservico de pedidos                       | 8084  | JPA, Flyway, Eureka Client, ModelMapper, Lombok, Testcontainers      |

---

## Criterios de Aceite

- [ ] Todos os POMs dos modulos atualizados para Spring Boot 4.0.x e Java 26
- [ ] Propriedade `<java.version>` alterada de `25` para `26` em todos os POMs
- [ ] `spring-boot-starter-parent` atualizado para versao `4.0.x` em todos os POMs
- [ ] Spring Cloud BOM (`spring-cloud.version`) atualizado para versao compativel com Spring Boot 4
- [ ] Resilience4j atualizado para versao compativel com Spring Boot 4 (verificar se `resilience4j-spring-boot3` foi renomeado para `resilience4j-spring-boot4` ou equivalente)
- [ ] Resilience4j BOM atualizado no `dependencyManagement` do modulo `pagamentos`
- [ ] Todos os Dockerfiles atualizados de `eclipse-temurin:25-jdk` / `eclipse-temurin:25-jre` para `eclipse-temurin:26-jdk` / `eclipse-temurin:26-jre`
- [ ] Todas as APIs deprecadas do Spring Boot 3.x substituidas pelos equivalentes do Spring Boot 4
- [ ] Todos os arquivos `application.yml` revisados para propriedades de configuracao renomeadas ou removidas no Spring Boot 4
- [ ] Compatibilidade do Eureka Server/Client verificada (ou migrado para alternativa, se necessario)
- [ ] Compatibilidade do Spring Cloud Gateway (WebFlux) verificada
- [ ] Compatibilidade do OpenFeign verificada
- [ ] Migracoes Flyway verificadas com a versao empacotada no Spring Boot 4
- [ ] Compatibilidade do Lombok com Java 26 verificada
- [ ] Compatibilidade do ModelMapper com Spring Boot 4 verificada
- [ ] Todos os testes unitarios e de integracao existentes passando
- [ ] Aplicacao inicia e executa com sucesso no ambiente Docker Compose
- [ ] `docker-compose.yml` revisado para garantir compatibilidade com as novas imagens

---

## Detalhes Tecnicos / Notas de Implementacao

### 1. Alteracoes nos POMs

Cada modulo possui seu proprio `pom.xml`. As seguintes alteracoes devem ser feitas em **todos os 4 modulos** (`server`, `gateway`, `pagamentos`, `pedidos`):

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
    <version>4.0.x</version> <!-- Substituir pela versao estavel mais recente -->
</parent>
<properties>
    <java.version>26</java.version>
    <spring-cloud.version>XXXX.X.X</spring-cloud.version> <!-- Versao compativel com SB 4 -->
</properties>
```

#### Modulo `pagamentos` - Alteracoes Adicionais
```xml
<!-- Verificar se o artefato mudou de nome -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId> <!-- Pode mudar para resilience4j-spring-boot4 -->
    <version>X.X.X</version> <!-- Versao compativel com SB 4 -->
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

### 2. Alteracoes nos Dockerfiles

Todos os 4 Dockerfiles seguem o mesmo padrao multi-stage. Atualizar em cada um:

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

### 3. Revisao dos `application.yml`

Verificar as notas de lancamento do Spring Boot 4 para propriedades renomeadas ou removidas. Arquivos a revisar:

| Modulo        | Arquivo                                          | Pontos de Atencao                                                |
| :------------ | :----------------------------------------------- | :--------------------------------------------------------------- |
| **server**    | `server/src/main/resources/application.yml`      | Configuracoes do Eureka Server                                   |
| **gateway**   | `gateway/src/main/resources/application.yml`     | `spring.cloud.gateway.discovery.locator` - verificar namespace   |
| **pagamentos**| `pagamentos/src/main/resources/application.yml`  | Config do Resilience4j, Flyway, JPA                              |
| **pedidos**   | `pedidos/src/main/resources/application.yml`     | Flyway, JPA                                                      |

### 4. Breaking Changes Potenciais a Investigar

- **Spring Boot 4**: Revisar o guia de migracao oficial (quando disponivel) para mudancas na API, remocao de classes deprecadas e alteracoes de comportamento padrao
- **Spring Cloud Gateway**: Verificar se `spring-cloud-starter-gateway-server-webflux` foi renomeado ou reestruturado
- **Netflix Eureka**: Verificar continuidade do suporte no ecossistema Spring Cloud compativel com SB 4
- **OpenFeign**: Verificar se `spring-cloud-starter-openfeign` continua sendo o artefato correto
- **Resilience4j**: O starter `resilience4j-spring-boot3` provavelmente tera uma nova versao especifica para SB 4
- **Lombok**: Verificar compatibilidade com Java 26 (novas features da linguagem podem impactar a geracao de codigo)
- **Flyway**: Verificar compatibilidade da versao empacotada com MySQL 8.0 e as migracoes existentes
- **Jakarta EE**: Verificar se houve atualizacao na versao das APIs Jakarta (Persistence, Validation, etc.)

### 5. Migracoes Flyway Existentes

As seguintes migracoes devem continuar funcionando apos a atualizacao:

- `pagamentos/src/main/resources/db/migration/V1__criar_tabela_pagamentos.sql`
- `pedidos/src/main/resources/db/migration/V1__cria_tabela_pedidos.sql`
- `pedidos/src/main/resources/db/migration/V2__cria_tabela_item_pedido.sql`

### 6. Docker Compose

O arquivo `docker-compose.yml` utiliza MySQL 8.0, que deve continuar compativel. Verificar se as imagens `eclipse-temurin:26` estao disponiveis nos builds dos servicos.

---

## Estrategia de Testes

1. **Testes unitarios**: Executar `mvn test` em cada modulo individualmente
2. **Testes de integracao**: Verificar testes com Testcontainers (modulos `pagamentos` e `pedidos`)
3. **Teste end-to-end**: Subir o ambiente completo via `docker-compose up` e validar:
   - Eureka Server acessivel em `http://localhost:8081`
   - Todos os servicos registrados no Eureka
   - Gateway roteando requisicoes corretamente
   - CRUD de pagamentos funcionando
   - CRUD de pedidos funcionando
   - Circuit Breaker do Resilience4j funcionando no fluxo pagamentos -> pedidos

---

## Riscos e Dependencias

| Risco                                                    | Mitigacao                                                   |
| :------------------------------------------------------- | :---------------------------------------------------------- |
| Spring Boot 4 ainda nao lancado ou em versao RC          | Aguardar versao GA ou utilizar versao RC com ciencia do time |
| Spring Cloud sem versao compativel com SB 4              | Verificar roadmap do Spring Cloud; considerar alternativas  |
| Resilience4j sem suporte a SB 4                          | Avaliar migracao para Spring Retry ou outra solucao         |
| Imagem `eclipse-temurin:26` nao disponivel               | Aguardar lancamento ou usar imagem alternativa (Azul Zulu)  |
| Lombok incompativel com Java 26                          | Considerar remocao gradual do Lombok usando records do Java |
| Quebras de API nao documentadas                          | Manter branch de migracao isolada para testes extensivos    |

---

## Definicao de Pronto (Definition of Done)

- Todos os microsservicos compilam com sucesso em Java 26 e Spring Boot 4
- Todos os testes unitarios e de integracao passam
- A aplicacao completa executa end-to-end no ambiente Docker Compose
- Nenhuma dependencia com vulnerabilidades conhecidas introduzida
- Codigo revisado por pelo menos um membro do time
- Documentacao (README, CHANGELOG) atualizada para refletir as novas versoes
