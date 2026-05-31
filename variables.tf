variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "eu-central-1"
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t2.micro"
}

variable "key_name" {
  description = "EC2 Key Pair name"
  type        = string
}

variable "db_password" {
  description = "Password for RDS database"
  type        = string
  sensitive   = true
}

variable "vpc_id" {
  description = "VPC ID existente"
  type        = string
}

variable "subnet_id" {
  description = "Subnet publica existente para EC2"
  type        = string
}

variable "private_subnet_1a" {
  description = "Subnet privada 1a para RDS"
  type        = string
}

variable "private_subnet_1b" {
  description = "Subnet privada 1b para RDS"
  type        = string
}

variable "security_group_id" {
  description = "Security Group existente"
  type        = string
}

variable "ami_id" {
  description = "AMI ID para Amazon Linux 2"
  type        = string
}