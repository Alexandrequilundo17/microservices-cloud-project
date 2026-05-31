resource "aws_db_subnet_group" "main" {
  name       = "week7-db-subnet-group"
  subnet_ids = [var.private_subnet_1a, var.private_subnet_1b]

  tags = {
    Name = "week7-db-subnet-group"
  }
}

resource "aws_db_instance" "main" {
  identifier              = "week7-database"
  engine                  = "postgres"
  engine_version          = "17"
  instance_class          = "db.t3.micro"
  allocated_storage       = 20
  storage_type            = "gp3"
  db_name                 = "week7db"
  username                = "postgres"
  password                = var.db_password
  vpc_security_group_ids  = [var.security_group_id]
  db_subnet_group_name    = aws_db_subnet_group.main.name
  backup_retention_period = 0
  storage_encrypted       = true
  publicly_accessible     = false
  skip_final_snapshot     = true

  tags = {
    Name = "week7-database"
  }
}