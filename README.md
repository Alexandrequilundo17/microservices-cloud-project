# Microservices Cloud Project

Arquitectura de microserviços cloud-native na AWS, desenvolvida para a cadeira **Sistemas de Informação na Nuvem** (Universidade Lusófona).

O sistema é composto por quatro microserviços Java/Spring Boot que comunicam de forma síncrona (REST) e assíncrona (AWS SQS), com infraestrutura totalmente gerida por código (Terraform), configuração automatizada (Ansible) e pipeline CI/CD (GitHub Actions).

---

## Visão Geral

| Serviço | Porta | Responsabilidade |
|---|---|---|
| api-gateway | 8080 | Ponto de entrada único, roteamento |
| user-service | 8081 | Gestão de utilizadores |
| product-service | 8082 | Gestão de produtos, consumer SQS (order-created), producer SQS (product-events) |
| order-service | 8083 | Gestão de encomendas, producer SQS (order-created), consumer SQS (product-events) |

## Stack Tecnológico

- **Linguagem:** Java 21, Spring Boot 3.4
- **Containerização:** Docker, Docker Compose
- **Registry:** DockerHub (`alexq113/*`)
- **IaC:** Terraform
- **Configuration Management:** Ansible
- **CI/CD:** GitHub Actions
- **Mensageria assíncrona:** AWS SQS
- **Base de dados:** AWS RDS PostgreSQL 17 (produção), H2 (desenvolvimento)
- **Cloud:** AWS (região eu-central-1)

---

## Estrutura do Repositório

```
microservices-cloud-project/
├── README.md
├── docs/
│   ├── architecture.md      # Componentes, fluxos de dados, diagramas
│   ├── setup.md             # Pré-requisitos locais e AWS
│   ├── deployment.md        # Passo-a-passo de deployment
│   ├── security.md          # IAM, secrets, least privilege
│   └── limitations.md       # Limitações conhecidas e melhorias futuras
├── services/
│   ├── api-gateway/
│   ├── user-service/
│   ├── product-service/
│   └── order-service/
├── infrastructure/
│   └── terraform/           # EC2, RDS, VPC, Security Groups
├── infra/
│   └── week9-sqs/           # Terraform das filas SQS
├── ansible/
│   ├── playbooks/
│   ├── roles/
│   └── inventory/
├── docker-compose.yml
└── .github/
    └── workflows/
        ├── ci.yml           # Build + testes (matrix)
        ├── image.yml        # Build + push DockerHub
        └── deploy.yml       # Deploy automático na EC2
```

---

## Como Correr

### Localmente

```bash
git clone https://github.com/Alexandrequilundo17/microservices-cloud-project
cd microservices-cloud-project
docker compose up --build
```

Os serviços ficam disponíveis em `http://localhost:8080-8083`.

### Na AWS (deployment automatizado)

```bash
cd ansible
ansible-playbook -i inventory/inventory.ini playbooks/playbook.yml -e @vars.yml
```

### Via CI/CD

Um `git push` para `main` despoleta automaticamente o pipeline: testes → build de imagens → push para DockerHub → deploy na EC2.

---

## GitHub Secrets Necessários

| Secret | Descrição |
|---|---|
| `DOCKERHUB_USERNAME` | Utilizador DockerHub |
| `DOCKERHUB_TOKEN` | Token de acesso DockerHub |
| `EC2_HOST` | IP público da EC2 |
| `EC2_SSH_KEY` | Chave privada SSH (week6-key) |

---

## Endpoints Principais

| Endpoint | Descrição |
|---|---|
| `GET /actuator/health` | Health check (todos os serviços) |
| `POST /products` | Criar produto (dispara evento SQS) |
| `POST /orders` | Criar encomenda (dispara evento SQS) |
| `GET /swagger-ui.html` | Documentação interactiva da API |

---

## Documentação Detalhada

- [Arquitectura](docs/architecture.md) — componentes e fluxos de dados
- [Setup](docs/setup.md) — pré-requisitos e configuração
- [Deployment](docs/deployment.md) — passo-a-passo de provisionamento
- [Segurança](docs/security.md) — IAM, secrets, least privilege
- [Limitações](docs/limitations.md) — limitações e melhorias futuras
