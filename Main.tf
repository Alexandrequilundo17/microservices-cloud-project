provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project   = "CloudComputing"
      Week      = "7"
      ManagedBy = "Terraform"
    }
  }
}

resource "aws_instance" "web" {
  ami                    = var.ami_id
  instance_type          = var.instance_type
  vpc_security_group_ids = [var.security_group_id]
  key_name               = var.key_name
  subnet_id              = var.subnet_id

  user_data = <<-USERDATA
    #!/bin/bash
    yum update -y
    yum install -y docker
    systemctl start docker
    systemctl enable docker
    usermod -aG docker ec2-user
  USERDATA

  tags = {
    Name = "Week7-Instance"
  }
}

resource "aws_eip" "web" {
  instance = aws_instance.web.id
  domain   = "vpc"

  tags = {
    Name = "week7-eip"
  }
}