# Setup

Guia de pré-requisitos e configuração para correr o projecto localmente e na AWS.

## Pré-requisitos Locais

| Ferramenta | Versão | Função |
|---|---|---|
| Java JDK | 21 | Compilar e correr os serviços |
| Maven | 3.9+ | Build dos projectos Java |
| Docker | 24+ | Containerização |
| Docker Compose | 2.x | Orquestração local |
| Git | 2.x | Controlo de versões |
| AWS CLI | 2.x | Interagir com a AWS |
| Terraform | 1.3+ | Provisionar infraestrutura |
| Ansible | 2.x | Configuração e deployment (requer Linux/WSL) |

### Notas de Plataforma

- **Ansible no Windows:** o Ansible não corre nativamente no Windows. Usar WSL (Windows Subsystem for Linux) com Ubuntu, ou correr via container Docker.
- **Java no Windows:** garantir que `JAVA_HOME` aponta para o JDK 21.

## Pré-requisitos AWS

### Conta e Credenciais

1. Conta AWS com acesso à região `eu-central-1`
2. Configurar credenciais:

```bash
aws configure
# AWS Access Key ID: <a tua key>
# AWS Secret Access Key: <o teu secret>
# Default region name: eu-central-1
# Default output format: json
```

### Recursos AWS Necessários

| Recurso | Descrição |
|---|---|
| VPC | Rede virtual com subnets públicas e privadas |
| EC2 | Instância para correr os containers (t3.micro) |
| RDS | PostgreSQL para persistência |
| SQS | Filas para comunicação assíncrona |
| Security Groups | Controlo de tráfego |
| IAM | Permissões least privilege para SQS |

### Permissões IAM

O utilizador/role precisa de permissões para:
- EC2 (criar, gerir instâncias)
- RDS (criar, gerir bases de dados)
- SQS (criar filas, enviar/receber mensagens)
- VPC (ler subnets, security groups)
- IAM (criar políticas)

## Configuração do Projecto

### 1. Clonar o repositório

```bash
git clone https://github.com/Alexandrequilundo17/microservices-cloud-project
cd microservices-cloud-project
```

### 2. Configurar variáveis Terraform

Criar `terraform.tfvars` em `infrastructure/terraform/`:

```hcl
db_password = "a-tua-password-segura"
key_name    = "week6-key"
ami_id      = "ami-0a628e1e89aaedf80"
subnet_id   = "subnet-069e30b8c1bd2ba91"
```

> **Importante:** `terraform.tfvars` está no `.gitignore` e nunca deve ser commitado.

### 3. Configurar chave SSH

```bash
# Copiar a chave para a localização do Ansible
cp /caminho/week6-key.pem ~/.ssh/
chmod 400 ~/.ssh/week6-key.pem
```

### 4. Variáveis de ambiente SQS

Para activar o SQS localmente:

```bash
export AWS_REGION=eu-central-1
export AWS_ACCESS_KEY_ID=<key>
export AWS_SECRET_ACCESS_KEY=<secret>
export CLOUD_SQS_PRODUCT_EVENTS_ENABLED=true
export CLOUD_SQS_PRODUCT_EVENTS_QUEUE_URL="https://sqs.eu-central-1.amazonaws.com/<account>/cn-course-product-events"
export CLOUD_SQS_PRODUCT_EVENTS_CONSUMER_ENABLED=true
export CLOUD_SQS_PRODUCT_EVENTS_CONSUMER_QUEUE_URL="$CLOUD_SQS_PRODUCT_EVENTS_QUEUE_URL"
export CLOUD_SQS_ORDER_CREATED_PUBLISHER_ENABLED=true
export CLOUD_SQS_ORDER_CREATED_CONSUMER_ENABLED=true
```

## Verificação

Confirmar que tudo está instalado:

```bash
java -version          # deve mostrar 21
mvn -version           # deve mostrar 3.9+
docker --version       # deve mostrar 24+
terraform -version     # deve mostrar 1.3+
ansible --version      # deve mostrar 2.x
aws sts get-caller-identity   # deve mostrar a tua conta AWS
```
