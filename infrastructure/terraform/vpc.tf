# ============================================================
# VPC existente — referenciada via data source
# Evita recriar infra que já existe na AWS
# ============================================================
data "aws_vpc" "main" {
  id = var.vpc_id
}

data "aws_subnet" "public" {
  id = var.subnet_id
}

data "aws_subnet" "private_1a" {
  id = var.private_subnet_1a
}

data "aws_subnet" "private_1b" {
  id = var.private_subnet_1b
}

# ============================================================
# Security Group para a EC2
# ============================================================
resource "aws_security_group" "ec2" {
  name        = "week7-ec2-sg"
  description = "Security group para EC2 microservices"
  vpc_id      = data.aws_vpc.main.id

  # HTTP — API Gateway
  ingress {
    from_port   = 8080
    to_port     = 8083
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # SSH — acesso para deploy
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Todo o tráfego de saída permitido
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "week7-ec2-sg"
  }
}

# ============================================================
# Security Group para o RDS
# ============================================================
resource "aws_security_group" "rds" {
  name        = "week7-rds-sg"
  description = "Security group para RDS PostgreSQL"
  vpc_id      = data.aws_vpc.main.id

  # PostgreSQL — só acessível pela EC2
  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ec2.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "week7-rds-sg"
  }
}
