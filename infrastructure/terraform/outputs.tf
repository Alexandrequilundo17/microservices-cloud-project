output "instance_id" {
  description = "ID of the EC2 instance"
  value       = aws_instance.web.id
}

output "instance_public_ip" {
  description = "Public IP address of the instance"
  value       = aws_eip.web.public_ip
}

output "instance_public_dns" {
  description = "Public DNS name of the instance"
  value       = aws_instance.web.public_dns
}

output "security_group_id" {
  description = "ID of the security group used"
  value       = var.security_group_id
}

output "rds_endpoint" {
  description = "RDS endpoint"
  value       = aws_db_instance.main.endpoint
}