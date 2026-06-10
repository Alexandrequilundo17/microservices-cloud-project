# Evidências do Projecto — Screenshots

## Sistemas de Informação na Nuvem
**Aluno:** Alexandre Quilundo
**Conta AWS:** 263050932702 (eu-central-1)
**Data:** 10 de Junho de 2026

Este documento organiza as evidências (screenshots) por requisito e por activity da ficha de SQS.

---

## 1. Filas SQS (AWS Console) — Activity 1

### 1.1 Configuração da fila principal
**Evidência:** Página de criação de fila com:
- Visibility timeout: **60 segundos** ✅
- Receive message wait time (long polling): **20 segundos** ✅
- Message retention: 4 dias

> *[Screenshot: Create queue — Configuration]*

### 1.2 Dead Letter Queue criada
**Evidência:** Fila `order-created-dlq` criada com sucesso.
- Type: Standard
- ARN: `arn:aws:sqs:eu-central-1:263050932702:order-created-dlq`

> *[Screenshot: Queue order-created-dlq created successfully]*

### 1.3 Redrive policy configurada
**Evidência:** Dead-letter queue ativada na fila principal apontando para a DLQ.
- Choose queue: `arn:aws:sqs:eu-central-1:263050932702:order-status-changed-dlq`
- Maximum receives: configurado

> *[Screenshot: Dead-letter queue — Enabled + Choose queue + Maximum receives]*

### Filas criadas no total
| Fila | Tipo | Função |
|---|---|---|
| `cn-course-product-events` | Standard | Eventos de produto (principal) |
| `cn-course-product-events-dlq` | Standard | DLQ dos eventos de produto |
| `order-created` | Standard | Eventos de encomenda (principal) |
| `order-created-dlq` | Standard | DLQ das encomendas |
| `order-status-changed` | Standard | Mudanças de estado |
| `order-status-changed-dlq` | Standard | DLQ das mudanças de estado |

---

## 2. Filas SQS (Terraform) — Activity 3

**Evidência:** `terraform apply` executado com sucesso, criando a fila principal e a DLQ com redrive policy.

Output do Terraform:
```
product_events_queue_url = "https://sqs.eu-central-1.amazonaws.com/263050932702/cn-course-product-events"
product_events_queue_arn = "arn:aws:sqs:eu-central-1:263050932702:cn-course-product-events"
product_events_dlq_url   = "https://sqs.eu-central-1.amazonaws.com/263050932702/cn-course-product-events-dlq"
product_events_dlq_arn   = "arn:aws:sqs:eu-central-1:263050932702:cn-course-product-events-dlq"
```

Atributos confirmados via CLI:
- VisibilityTimeout: 60
- ReceiveMessageWaitTimeSeconds: 20
- RedrivePolicy: maxReceiveCount 5 → DLQ

> *[Screenshot: terraform apply output com os 4 outputs]*

---

## 3. IAM — Least Privilege — Activity 4

### 3.1 Política anexada ao utilizador
**Evidência:** Política `microservices-sqs-policy` (Customer managed) anexada ao `Alex01_User`.

> *[Screenshot: Alex01_User permissions com microservices-sqs-policy + "1 policy added"]*

### 3.2 Detalhe da política least privilege
**Evidência:** Política concede apenas acesso SQS limitado (Read, Write) na região eu-central-1.
- Type: Customer managed
- Service: SQS
- Access level: Limited (Read, Write)
- ARN: `arn:aws:iam::263050932702:policy/microservices-sqs-policy`

> *[Screenshot: microservices-sqs-policy — Permissions defined]*

**Nota:** a conta tem um permissions boundary que restringe operações SQS via IAM user. Documentado em `limitations.md`.

---

## 4. Fluxo SQS Funcional — Activity 5 (EVIDÊNCIA PRINCIPAL)

**Evidência:** Comunicação assíncrona producer → SQS → consumer comprovada nos logs.

### Producer (product-service)
```
2026-06-10T11:23:01.674Z DEBUG 1 --- [product-service] [nio-8082-exec-4]
p.u.p.sqs.ProductEventSqsPublisher : Published ProductCreatedSqsEvent for productId=1
```

### Consumer (order-service)
```
2026-06-10T11:23:06.721Z INFO 1 --- [order-service] [scheduling-1]
p.u.o.s.ProductEventSqsPollingConsumer : SQS product event: type=ProductCreated
productId=1 name=Produto Teste SQS price=29.99
```

### Pedido que despoletou o fluxo
```
POST /products
{"id":1,"name":"Produto Teste SQS","price":29.99,"stockQuantity":100,...}
```

> *[Screenshot: Docker Desktop com logs do producer e consumer — Imagem principal]*

**Esta é a prova definitiva de que a arquitectura event-driven com SQS funciona.**

---

## 5. Aplicação Funcional na EC2

### 5.1 Health checks dos 4 serviços
**Evidência:** Os 4 serviços respondem `{"status":"UP"}` na EC2 (63.184.120.198).
- api-gateway :8080 → UP
- user-service :8081 → UP
- product-service :8082 → UP
- order-service :8083 → UP

> *[Screenshot: curl actuator/health dos 4 serviços com status UP]*

### 5.2 Criação de produto na EC2
**Evidência:** POST /products na EC2 devolve produto criado.
```
{"id":1,"name":"Produto Demo SQS","price":49.99,"stockQuantity":50,...}
```

> *[Screenshot: curl POST products na EC2]*

---

## 6. Infraestrutura AWS

### 6.1 Instância EC2 a correr
**Evidência:** Instância `microservice-test` (i-0cd9adf001048fe64) em estado **Running**, t3.micro, eu-central-1a.

> *[Screenshot: EC2 Instances com microservice-test Running]*

### 6.2 Security Group
**Evidência:** Security group com regras de acesso configuradas.

> *[Screenshot: Security group rules]*

---

## 7. Migração Kafka → SQS

**Evidência:** Código migrado de Kafka para SQS. O `@EnableScheduling` está activo no order-service para o polling SQS.
```
order-service/.../OrderServiceApplication.java:8: import ...EnableScheduling;
order-service/.../OrderServiceApplication.java:44: @EnableScheduling
```

Build e testes a passar:
- order-service: BUILD SUCCESS (15+36 testes)
- product-service: BUILD SUCCESS (20+34 testes)

> *[Screenshot: terminal com grep EnableScheduling + build success]*

---

## Mapeamento Evidências → Activities da Ficha

| Activity | Requisito | Evidência |
|---|---|---|
| 1 | Filas SQS (consola) + DLQ + redrive + visibility 60s + long polling 20s | Secção 1 ✅ |
| 2 | Filas SQS (CLI) | get-queue-attributes output ✅ |
| 3 | Filas SQS (Terraform) | Secção 2 ✅ |
| 4 | IAM least privilege | Secção 3 ✅ |
| 5 | Variáveis de ambiente + logs do consumer | Secção 4 ✅ |
| 6 | Dead Letter Queue | Filas DLQ criadas (Secção 1) ✅ |
| 7 | FIFO (opcional) | Não implementado (opcional) |

---

## Notas de Limpeza (pós-entrega)

- ⚠️ Verificar/desligar instância **m7i-flex.large** (visível nos screenshots) — não é Free Tier
- ⚠️ Revogar o access key AWS exposto durante o desenvolvimento e criar novo
- Desligar instâncias EC2 não utilizadas para preservar créditos
