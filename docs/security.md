# Segurança

Decisões de segurança, gestão de credenciais e princípios de least privilege aplicados no projecto.

## Princípios Aplicados

1. **Least privilege** — cada identidade tem só as permissões mínimas necessárias
2. **Sem credenciais hardcoded** — todas as credenciais vêm de variáveis de ambiente ou secrets
3. **Isolamento de rede** — RDS não acessível publicamente, só dentro da VPC
4. **Secrets fora do controlo de versões** — `.gitignore` protege ficheiros sensíveis

## Gestão de Credenciais

### Variáveis de Ambiente

Credenciais AWS e tokens nunca estão no código. São injectadas via:
- Variáveis de ambiente no shell (desenvolvimento)
- GitHub Secrets (CI/CD)
- Ficheiro `.env` na EC2 (produção, não commitado)

### Ficheiros Protegidos (.gitignore)

```
terraform.tfvars       # contém db_password
*.tfstate              # pode conter dados sensíveis
*.pem                  # chaves SSH
vars.yml               # token DockerHub
.env                   # credenciais AWS
```

### GitHub Secrets

Credenciais do CI/CD guardadas encriptadas no GitHub:

| Secret | Uso |
|---|---|
| `DOCKERHUB_USERNAME` | Login no registry |
| `DOCKERHUB_TOKEN` | Autenticação (revogável) |
| `EC2_HOST` | Endereço de deploy |
| `EC2_SSH_KEY` | Acesso SSH à EC2 |

## IAM — Least Privilege

### Política SQS

A política `microservices-sqs-policy` concede apenas as acções necessárias:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "sqs:SendMessage",
        "sqs:ReceiveMessage",
        "sqs:DeleteMessage",
        "sqs:GetQueueUrl",
        "sqs:GetQueueAttributes"
      ],
      "Resource": "arn:aws:sqs:eu-central-1:*:cn-course-*"
    }
  ]
}
```

**Justificação das permissões:**
- `sqs:SendMessage` + `sqs:GetQueueUrl` — necessárias para o producer
- `sqs:ReceiveMessage` + `sqs:DeleteMessage` + `sqs:GetQueueAttributes` — necessárias para o consumer

Não são concedidas permissões de criação/eliminação de filas ao runtime — essas operações são feitas via Terraform com credenciais administrativas separadas.

## Segurança de Rede

### Security Groups

| Security Group | Regras Inbound |
|---|---|
| EC2 | SSH (22), HTTP serviços (8080-8083) |
| RDS | PostgreSQL (5432) apenas do security group da EC2 |

### Isolamento do RDS

O RDS tem `publicly_accessible = false` — só é acessível de dentro da VPC, pela EC2. A base de dados nunca está exposta à internet.

## Containerização Segura

- **Multi-stage builds** — a imagem final não contém ferramentas de build (Maven, JDK completo), apenas o JRE e o `.jar`
- **Imagens base mínimas** — Alpine para reduzir superfície de ataque

## Limitações de Segurança Conhecidas

### Permissions Boundary

A conta universitária aplica um **permissions boundary** que bloqueia operações SQS via o IAM user `Alex01_User`, independentemente das políticas anexadas. Para criar as filas, foi necessário usar credenciais da conta root.

**Melhoria recomendada para produção:** usar um **IAM Role anexado à EC2** (instance profile) em vez de credenciais directas. Assim, a EC2 obtém credenciais temporárias automaticamente, sem necessidade de armazenar chaves de acesso.

### Credenciais na EC2

Actualmente as credenciais AWS são passadas via ficheiro `.env` na EC2. Em produção, o instance profile eliminaria esta necessidade, sendo a abordagem recomendada pela AWS.
