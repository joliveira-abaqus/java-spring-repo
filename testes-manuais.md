# Testes Manuais - Autenticacao JWT

## Pre-requisitos

- Docker e Docker Compose instalados
- Subir todos os servicos: `docker compose up --build -d`
- Aguardar ~30s para todos os servicos inicializarem

---

## 1. Acesso SEM token (deve retornar 401)

```bash
curl -i http://localhost:8082/pagamentos-ms/pagamentos
```

**Esperado:** `401 Unauthorized`

---

## 2. Registrar um usuario

```bash
curl -i -X POST http://localhost:8082/auth-ms/auth/registro \
  -H "Content-Type: application/json" \
  -d '{"nome":"Admin","email":"admin@alurafood.com","senha":"123456"}'
```

**Esperado:** `200 OK` com JSON:

```json
{
  "token": "<JWT_TOKEN>",
  "nome": "Admin",
  "email": "admin@alurafood.com",
  "role": "ROLE_USER"
}
```

---

## 3. Fazer login

```bash
curl -i -X POST http://localhost:8082/auth-ms/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@alurafood.com","senha":"123456"}'
```

**Esperado:** `200 OK` com JSON contendo o `token`

---

## 4. Acessar pagamentos COM token valido

Substitua `<SEU_TOKEN>` pelo valor recebido no passo 2 ou 3.

```bash
curl -i http://localhost:8082/pagamentos-ms/pagamentos \
  -H "Authorization: Bearer <SEU_TOKEN>"
```

**Esperado:** `200 OK` com a lista de pagamentos

---

## 5. Acessar pedidos COM token valido

```bash
curl -i http://localhost:8082/pedidos-ms/pedidos \
  -H "Authorization: Bearer <SEU_TOKEN>"
```

**Esperado:** `200 OK` com a lista de pedidos

---

## 6. Acesso com token INVALIDO (deve retornar 401)

```bash
curl -i http://localhost:8082/pagamentos-ms/pagamentos \
  -H "Authorization: Bearer token_invalido_qualquer"
```

**Esperado:** `401 Unauthorized`

---

## 7. Validar token diretamente

```bash
curl -i "http://localhost:8082/auth-ms/auth/validar?token=<SEU_TOKEN>"
```

**Esperado:** `200 OK` se valido, `401 Unauthorized` se invalido

---

## 8. Registrar com email duplicado (deve falhar)

```bash
curl -i -X POST http://localhost:8082/auth-ms/auth/registro \
  -H "Content-Type: application/json" \
  -d '{"nome":"Admin2","email":"admin@alurafood.com","senha":"654321"}'
```

**Esperado:** Erro indicando que o email ja esta cadastrado

---

## 9. Login com senha errada (deve falhar)

```bash
curl -i -X POST http://localhost:8082/auth-ms/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@alurafood.com","senha":"senhaerrada"}'
```

**Esperado:** `401 Unauthorized` ou `403 Forbidden`

---

## Resumo

| #  | Cenario                          | Esperado          |
|----|----------------------------------|--------------------|
| 1  | GET /pagamentos sem token        | 401 Unauthorized   |
| 2  | POST /auth/registro              | 200 OK + token     |
| 3  | POST /auth/login                 | 200 OK + token     |
| 4  | GET /pagamentos com token valido | 200 OK             |
| 5  | GET /pedidos com token valido    | 200 OK             |
| 6  | GET /pagamentos com token falso  | 401 Unauthorized   |
| 7  | GET /auth/validar com token      | 200 OK             |
| 8  | POST /auth/registro email duplo  | Erro (duplicado)   |
| 9  | POST /auth/login senha errada    | 401/403            |
