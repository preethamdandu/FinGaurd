# Production environment – remote state is mandatory.

terraform {
  backend "s3" {
    bucket         = "fingaurd-terraform-state"
    key            = "prod/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "fingaurd-terraform-locks"
  }
}
