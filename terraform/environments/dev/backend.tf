# Development environment – use local state for simplicity.
# For shared teams, uncomment the S3 backend below.

# terraform {
#   backend "s3" {
#     bucket         = "fingaurd-terraform-state"
#     key            = "dev/terraform.tfstate"
#     region         = "us-east-1"
#     encrypt        = true
#     dynamodb_table = "fingaurd-terraform-locks"
#   }
# }
