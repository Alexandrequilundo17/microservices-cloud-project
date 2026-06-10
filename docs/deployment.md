# Deployment

Guia passo-a-passo do provisionamento de infraestrutura ao deployment dos serviços.

## Visão Geral do Fluxo

```
Terraform (provisiona infra) → Ansible (configura EC2 + deploy) → CI/CD (deploy contínuo)
```

## 1. Provisionar Infraestrutura (Terraform)

### Infraestrutura principal (EC2, RDS, VPC, Security Groups)

```bash
cd infrastructure/terraform
terraform init
terraform plan
terraform apply
```

Outputs relevantes:
- `instance_public_ip` — IP da EC2
- `rds_endpoint` — endpoint do RDS
- `security_group_id` — ID do security group

### Filas SQS

```bash
cd infra/week9-sqs
terraform init
terraform plan
terraform apply
```

Outputs:
- `product_events_queue_url` — URL da fila principal
- `product_events_dlq_url` — URL da dead letter queue

## 2. Build e Push das Imagens (Docker)

```bash
# Login no DockerHub
docker login -u <username>

# Build e push de cada serviço
docker build -t <username>/user-service:1.0 ./user-service
docker push <username>/user-service:1.0

# Repetir para product-service, order-service, api-gateway
```

Ou usar o script:

```bash
./build-and-push.sh
```

## 3. Configurar e Deploy na EC2 (Ansible)

### Actualizar o inventory

Editar `ansible/inventory/inventory.ini` com o IP da EC2:

```ini
[web_servers]
ec2 ansible_host=<IP_DA_EC2> ansible_user=ubuntu

[all:vars]
ansible_ssh_private_key_file=~/.ssh/week6-key.pem
ansible_python_interpreter=/usr/bin/python3
```

### Correr o playbook

```bash
cd ansible
ansible-playbook -i inventory/inventory.ini playbooks/playbook.yml -e @vars.yml
```

O playbook executa:
1. Actualizar pacotes do sistema
2. Instalar Docker e Docker Compose
3. Copiar `docker-compose.yml` para a EC2
4. Login no DockerHub
5. Arrancar os containers
6. Criar serviço systemd para auto-start no boot

## 4. CI/CD Automático (GitHub Actions)

Após configurar os GitHub Secrets, qualquer `git push` para `main` despoleta:

```
push → ci.yml (testes) → image.yml (build + push) → deploy.yml (deploy EC2)
```

### Configurar Secrets

No repositório GitHub → Settings → Secrets and variables → Actions:

| Secret | Valor |
|---|---|
| `DOCKERHUB_USERNAME` | utilizador DockerHub |
| `DOCKERHUB_TOKEN` | token DockerHub |
| `EC2_HOST` | IP da EC2 |
| `EC2_SSH_KEY` | conteúdo da chave privada |

## 5. Verificar o Deployment

```bash
# Health checks
curl http://<IP_DA_EC2>:8080/actuator/health
curl http://<IP_DA_EC2>:8081/actuator/health
curl http://<IP_DA_EC2>:8082/actuator/health
curl http://<IP_DA_EC2>:8083/actuator/health
```

Todos devem responder `{"status":"UP"}`.

### Testar o fluxo SQS

```bash
# Criar um produto (dispara evento SQS)
curl -X POST http://<IP_DA_EC2>:8082/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Produto Teste","description":"Teste SQS","price":29.99,"stockQuantity":100}'

# Ver o consumo no order-service
ssh -i ~/.ssh/week6-key.pem ubuntu@<IP_DA_EC2> \
  "cd /opt/microservices/app && docker compose logs order-service | grep -i sqs"
```

Deve aparecer: `SQS product event: type=ProductCreated productId=1 ...`

## Notas Operacionais

- **Espaço em disco:** a `t3.micro` tem 8GB. Correr `docker system prune -af` periodicamente para libertar espaço.
- **Elastic IP:** associar um Elastic IP à instância para o IP não mudar entre reinícios.
- **Auto-start:** o serviço systemd garante que os containers arrancam após reinício da EC2.
