Auth API

API REST de autenticação e autorização construída com **Java 21** e **Spring Boot**, com foco em **segurança stateless com JWT** e **mensageria resiliente com RabbitMQ (retry + Dead Letter Queue)**.

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen?logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-JWT-6DB33F?logo=springsecurity&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-4-FF6600?logo=rabbitmq&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-Migrations-red?logo=flyway&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![Testcontainers](https://img.shields.io/badge/Testcontainers-Integration%20Tests-9B489A)
 
---

## 📌 Sobre o projeto

A Auth API implementa o núcleo de autenticação de uma aplicação: cadastro de usuário, login com emissão de token e proteção de rotas por JWT. O cadastro não termina no banco — ele **publica um evento `UserRegisteredEvent` no RabbitMQ**, desacoplando efeitos colaterais (e-mail de boas-vindas, onboarding, analytics) do fluxo síncrono da requisição. O consumidor tem **retry com backoff exponencial e Dead Letter Queue**, então uma mensagem envenenada não se perde nem trava a fila.

### Principais destaques técnicos

- **Autenticação stateless com JWT** — token assinado com HMAC256 (Auth0 `java-jwt`), com `issuer` e `subject` validados. A escolha de HMAC sobre RSA é deliberada: um único serviço assina e valida os tokens, então não há necessidade de distribuir chave pública
- **Sessão `STATELESS` no Spring Security** — nenhuma sessão em memória, nenhum `JSESSIONID`; o servidor pode escalar horizontalmente sem session affinity
- **Filtro de autenticação customizado** — `SecurityFilter` estende `OncePerRequestFilter`, extrai o `Bearer token`, valida a assinatura e popula o `SecurityContextHolder` antes do `UsernamePasswordAuthenticationFilter`
- **Senhas com BCrypt** — hash com salt automático; o campo `password` tem `@ToString.Exclude` para não vazar em log
- **Proteção contra enumeração de usuários** — credencial inválida e usuário inexistente retornam o mesmo `401 Unauthorized` com mensagem genérica, sem revelar qual dos dois falhou
- **Proteção contra mass assignment** — a `role` é fixada no servidor como `CLIENT` no momento do cadastro e ignorada do payload, impedindo que um cliente se autopromova a `ADMIN`
- **Evento assíncrono com fanout exchange** — o `register` publica em `auth.exchange`; a escolha de fanout permite plugar novos consumidores (e-mail, CRM, auditoria) sem alterar o produtor
- **Retry com backoff + DLQ** — 3 tentativas com intervalo crescente (1s → 2s → 4s); esgotadas as tentativas, a mensagem é roteada para `user.registered.dlq` em vez de ser descartada
- **Tratamento de erros centralizado** — `@RestControllerAdvice` traduz validação, violação de integridade e falha de autenticação em um payload de erro padronizado (`400` / `409` / `401`)
- **Unicidade garantida no banco** — `user_name` e `email` têm constraint `UNIQUE` no PostgreSQL; a checagem não depende de uma consulta prévia na aplicação, que seria vulnerável a race condition
- **Schema versionado com Flyway** — 7 migrations versionadas em schema dedicado (`auth`), com `ddl-auto=validate` para que o Hibernate nunca altere o banco por conta própria
- **Configuração via variáveis de ambiente** — nenhuma credencial ou segredo de assinatura commitado no repositório
---

## 🛠️ Tecnologias

| Tecnologia | Uso |
|---|---|
| Java 21 | Linguagem |
| Spring Boot 4.x (Web MVC, Data JPA, Validation) | Framework da aplicação |
| Spring Security | Autenticação, autorização e filter chain |
| Auth0 `java-jwt` 4.6.0 | Emissão e validação dos tokens JWT |
| BCrypt | Hash de senhas |
| PostgreSQL 16 | Banco de dados relacional |
| Flyway | Migrations e versionamento de schema |
| RabbitMQ 4 | Mensageria (fanout exchange, DLQ) |
| Docker Compose | Provisionamento de PostgreSQL e RabbitMQ |
| Testcontainers + JUnit 5 | Testes de integração com banco real |
| Mockito | Testes unitários |
| Lombok | Redução de boilerplate |
| Maven (wrapper incluso) | Build e gerenciamento de dependências |
 
---

## 🏗️ Arquitetura

O projeto segue uma arquitetura em camadas, com a configuração de segurança e de mensageria isoladas:

```
src/main/java/com/lucasmarques/authapi
├── config/         # SecurityConfig, SecurityFilter, RabbitMQConfiguration
├── controller/     # Endpoints REST (AuthController, UserController)
├── service/        # AuthService, TokenService, JpaUserDetailsService
├── repository/     # Acesso a dados via Spring Data JPA
├── entity/         # Entidade JPA User (implementa UserDetails)
├── dto/            # Records de request, response e evento
├── consumer/       # RabbitConsumer (fila principal + DLQ)
├── enums/          # UserRole (ADMIN, CLIENT)
└── exception/      # GlobalExceptionHandler (@RestControllerAdvice)
```

### Modelo de dados

Tabela `users`, no schema `auth`:

| Coluna | Tipo | Restrições |
|---|---|---|
| `id` | UUID | PK, gerado pela aplicação |
| `user_name` | VARCHAR | `NOT NULL`, `UNIQUE` |
| `password` | VARCHAR(60) | `NOT NULL` (hash BCrypt) |
| `email` | VARCHAR(255) | `NOT NULL`, `UNIQUE` |
| `role` | VARCHAR(20) | `ADMIN` ou `CLIENT`, default `CLIENT` |

### Fluxo de mensageria

```
POST /auth/register
        │
        ├──► users (PostgreSQL)
        │
        └──► auth.exchange (fanout)
                    │
                    └──► user.registered  ──(3 tentativas, backoff 1s→2s→4s)──► RabbitConsumer
                                │
                                └── falhou nas 3 ──► auth.exchange.dlq ──► user.registered.dlq
```
 
---

## 🔗 Endpoints

### Cadastrar usuário

```http
POST /auth/register
Content-Type: application/json
```

```json
{
  "userName": "lucas",
  "password": "senhaSegura123",
  "email": "lucas@email.com"
}
```

| Resposta | Situação |
|---|---|
| `201 Created` | Usuário cadastrado e evento publicado (sem corpo de resposta) |
| `400 Bad Request` | Campos inválidos — username em branco, e-mail malformado ou senha com menos de 8 caracteres |
| `409 Conflict` | `userName` ou `email` já cadastrado |

### Login

```http
POST /auth/login
Content-Type: application/json
```

```json
{
  "userName": "lucas",
  "password": "senhaSegura123"
}
```

Resposta `200 OK`:

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userName": "lucas",
  "expiration": "2026-01-15T18:45:00Z"
}
```

| Resposta | Situação |
|---|---|
| `200 OK` | Autenticado — retorna o token e o instante de expiração (15 minutos) |
| `401 Unauthorized` | Credenciais inválidas (mensagem genérica, sem distinguir usuário inexistente de senha errada) |

### Consultar usuário autenticado

```http
GET /user/me
Authorization: Bearer <token>
```

Resposta `200 OK`:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "username": "lucas",
  "role": "CLIENT",
  "email": "lucas@email.com"
}
```

| Resposta | Situação |
|---|---|
| `200 OK` | Retorna os dados do usuário dono do token |
| `403 Forbidden` | Token ausente, expirado ou com assinatura inválida |

> Qualquer rota fora de `/auth/register` e `/auth/login` exige token válido.
 
---

## 🚀 Como executar

### Pré-requisitos

- Java 21+
- Docker e Docker Compose
- (Opcional) Maven — o projeto inclui o Maven Wrapper (`./mvnw`)
### 1. Clone o repositório

```bash
git clone https://github.com/Lmsantoz/auth-api.git
cd auth-api
```

### 2. Configure as variáveis de ambiente

Crie um arquivo `.env` na raiz do projeto (ou exporte as variáveis no shell):

```env
# PostgreSQL
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_DB=auth_db
POSTGRES_URL=jdbc:postgresql://localhost:5423/auth_db
 
# JWT — use um segredo forte e aleatório, nunca o do exemplo
JWT_TOKEN=troque-este-valor-por-um-segredo-aleatorio
 
# RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest
```

> O PostgreSQL expõe a porta **5423** no host para evitar conflito com instâncias locais rodando na 5432.

### 3. Suba a infraestrutura

```bash
docker compose up -d
```

Isso levanta o PostgreSQL e o RabbitMQ. O painel de administração do RabbitMQ fica em `http://localhost:15672`.

### 4. Execute a aplicação

```bash
./mvnw spring-boot:run
```

As migrations do Flyway rodam automaticamente na inicialização, criando o schema `auth` e a tabela `users`. As filas, exchanges e bindings do RabbitMQ são declarados pela aplicação na subida.

A API estará disponível em `http://localhost:8080`.

### 5. Teste o fluxo completo

```bash
# cadastra
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"userName":"lucas","password":"senhaSegura123","email":"lucas@email.com"}'
 
# faz login e captura o token
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"userName":"lucas","password":"senhaSegura123"}' | jq -r .token)
 
# acessa a rota protegida
curl http://localhost:8080/user/me -H "Authorization: Bearer $TOKEN"
```
 
---

## ✅ Testes

```bash
./mvnw test
```

Os testes de integração usam **Testcontainers**: um PostgreSQL real é criado automaticamente durante a execução, sem configuração manual de banco — basta ter o Docker rodando. O RabbitMQ é mockado nesses testes, já que o objetivo ali é validar o fluxo HTTP e a persistência.

**Testes unitários (Mockito)**

- **Hash antes de persistir** — captura o `User` enviado ao repositório e valida que a senha gravada é o hash, nunca o texto puro
- **Role fixada no servidor** — valida que o usuário criado recebe `CLIENT` independentemente do payload
- **Publicação do evento** — valida que o `RabbitTemplate` publica na exchange correta com o `UserRegisteredEvent`
- **Login com sucesso** — valida que o token gerado é devolvido na resposta
- **Credencial inválida** — valida que `BadCredentialsException` é propagada para o handler global
  **Testes de integração (Testcontainers)**

- **Cadastro com dados válidos** — `POST /auth/register` contra banco real retornando `201 Created`
- **Ciclo completo de autenticação** — cadastra, faz login, extrai o token da resposta e acessa `/user/me` com o header `Authorization`, validando a filter chain de ponta a ponta
---

## 🗺️ Próximos passos

- [ ] Padrão Outbox para garantir consistência entre a persistência do usuário e a publicação do evento
- [ ] `AuthenticationEntryPoint` customizado para responder `401` com payload padronizado em vez do `403` default do Spring Security
- [ ] Tratamento específico por tipo de falha na validação do token (expirado, assinatura inválida, malformado)
- [ ] Refresh token com rotação, reduzindo a exposição do access token
- [ ] Mitigação de timing attack no login (hash dummy quando o usuário não existe)
- [ ] Rate limiting no endpoint de login para conter brute force
- [ ] Documentação interativa com Swagger / OpenAPI
- [ ] Teste de integração cobrindo o caminho da DLQ com container real do RabbitMQ
---

## 📄 Licença

Este projeto está sob a licença descrita no arquivo [LICENSE](LICENSE).
 
---

## 👤 Autor

**Lucas Marques**

[![GitHub](https://img.shields.io/badge/GitHub-Lmsantoz-181717?logo=github)](https://github.com/Lmsantoz)