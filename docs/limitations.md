# Limitações e Melhorias Futuras

Limitações conhecidas do projecto e roadmap de melhorias.

## Limitações Conhecidas

### 1. Permissions Boundary da Conta

A conta AWS universitária tem um **permissions boundary** que bloqueia o IAM user (`Alex01_User`) de executar operações SQS (`sqs:CreateQueue`, `sqs:GetQueueAttributes`, etc.), mesmo com a política correcta anexada.

**Impacto:** as filas SQS tiveram de ser criadas com credenciais da conta root.

**Solução em produção:** um IAM Role anexado à EC2 (instance profile) com as permissões necessárias, sem permissions boundary restritivo.

### 2. Recursos da Instância (Free Tier)

A conta AWS Free Tier limita o tipo de instância a `t3.micro` (1GB RAM, 8GB disco). Esta limitação causou dois problemas:

- **RAM insuficiente** para correr Kafka + Zookeeper + 4 serviços Java simultaneamente
- **Espaço em disco insuficiente** ao puxar as imagens do Kafka

**Solução aplicada:** migração de Kafka para AWS SQS, removendo 2 containers pesados (Kafka + Zookeeper) e tornando a arquitectura cloud-native. O deployment passou a caber confortavelmente na `t3.micro`.

### 3. Credenciais via .env na EC2

As credenciais AWS são actualmente passadas à aplicação via ficheiro `.env` na EC2.

**Melhoria recomendada:** IAM Role / instance profile, que fornece credenciais temporárias rotativas automaticamente, eliminando o armazenamento de chaves estáticas.

### 4. Idempotência das Mensagens SQS

O SQS Standard garante entrega "at least once" — uma mensagem pode ser entregue mais do que uma vez.

**Estado actual:** o consumer não implementa verificação de idempotência.

**Melhoria recomendada:** registar IDs de mensagens já processadas (ex: numa tabela) e ignorar duplicados, garantindo que o stock não é decrementado duas vezes pelo mesmo evento.

### 5. Terraform Modules

O código Terraform actual está numa estrutura simples (recursos directos), não modularizado.

**Melhoria recomendada:** organizar em módulos reutilizáveis (`modules/ec2`, `modules/rds`, `modules/sqs`) com ambientes separados (`environments/dev`, `environments/prod`) e remote state (S3 backend).

### 6. EC2 Instance Connect

Uma das instâncias EC2 apresentou problemas intermitentes com o EC2 Instance Connect (falha de ligação SSH apesar da instância estar saudável).

**Workaround aplicado:** injecção da chave SSH pública via `user_data` do Terraform, garantindo acesso permanente independentemente do EC2 Instance Connect.

## Melhorias Futuras (Roadmap)

### Infraestrutura
- [ ] Modularizar o Terraform (modules + environments + remote state)
- [ ] IAM Role / instance profile em vez de credenciais estáticas
- [ ] Auto-scaling group para alta disponibilidade
- [ ] Application Load Balancer à frente dos serviços

### Observabilidade
- [ ] Stack de monitoring (Prometheus + Grafana)
- [ ] Centralização de logs (CloudWatch ou ELK)
- [ ] Alarmes para profundidade da fila SQS e mensagens na DLQ

### Resiliência
- [ ] Idempotência no consumer SQS
- [ ] Circuit breakers nas chamadas REST (Resilience4j)
- [ ] Retry policies configuráveis

### CI/CD
- [ ] AWS OIDC em vez de credenciais nos secrets
- [ ] Environment protection rules (aprovação manual para produção)
- [ ] Reusable workflows
- [ ] Terraform plan/apply automatizado no pipeline

### Segurança
- [ ] Secrets Manager para gestão centralizada de credenciais
- [ ] Encriptação em trânsito (TLS) entre serviços
- [ ] WAF no API Gateway

## Conclusão

O projecto cumpre os requisitos fundamentais de uma arquitectura cloud-native: infraestrutura como código, containerização, comunicação síncrona e assíncrona, persistência gerida, automação de deployment e CI/CD. As limitações identificadas decorrem maioritariamente das restrições da conta Free Tier universitária e representam oportunidades claras de evolução para um ambiente de produção real.
