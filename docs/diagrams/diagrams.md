# Diagramas do Sistema

Este ficheiro contém os diagramas da arquitectura em formato Mermaid (renderizam automaticamente no GitHub).

## 1. Arquitectura Geral

```mermaid
graph TB
    Client[Cliente]
    GW[API Gateway :8080]
    US[User Service :8081]
    PS[Product Service :8082]
    OS[Order Service :8083]
    RDS[(RDS PostgreSQL)]

    Client --> GW
    GW --> US
    GW --> PS
    GW --> OS

    OS -->|REST: valida utilizador| US
    OS -->|REST: valida produto| PS

    US --> RDS
    PS --> RDS
    OS --> RDS
```

## 2. Comunicação Assíncrona (AWS SQS)

```mermaid
graph LR
    OS[Order Service]
    PS[Product Service]
    Q1[SQS: order-created]
    Q2[SQS: product-events]
    DLQ1[DLQ: order-created-dlq]
    DLQ2[DLQ: product-events-dlq]

    OS -->|publica| Q1
    Q1 -->|consome, actualiza stock| PS
    Q1 -.5 falhas.-> DLQ1

    PS -->|publica| Q2
    Q2 -->|consome, notifica| OS
    Q2 -.5 falhas.-> DLQ2
```

## 3. Fluxo de Deployment

```mermaid
graph LR
    TF[Terraform] -->|provisiona| INFRA[EC2 + RDS + VPC + SQS]
    ANS[Ansible] -->|configura + deploy| INFRA
    GH[GitHub Actions] -->|CI/CD| DH[DockerHub]
    DH -->|pull| INFRA
```

## 4. Pipeline CI/CD

```mermaid
graph LR
    PUSH[git push main] --> CI[ci.yml: testes]
    CI --> IMG[image.yml: build + push]
    IMG --> DH[DockerHub]
    IMG --> DEP[deploy.yml: SSH EC2]
    DEP --> EC2[Containers actualizados]
```

## 5. Infraestrutura de Rede (VPC)

```mermaid
graph TB
    subgraph VPC[VPC vpc-08b59a722733074df]
        subgraph PUB[Subnet Pública]
            EC2[EC2 t3.micro<br/>Elastic IP]
        end
        subgraph PRIV[Subnets Privadas]
            RDS[(RDS PostgreSQL<br/>não público)]
        end
    end
    IGW[Internet Gateway] --> PUB
    EC2 -->|porta 5432| RDS
    Internet[Internet] --> IGW
```
