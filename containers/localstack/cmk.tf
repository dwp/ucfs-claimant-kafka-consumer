provider "aws" {
  access_key                  = "access_key_id"
  region                      = "eu-west-2"
  s3_force_path_style         = true
  secret_key                  = "secret_access_key"
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true

  endpoints {
    kms            = "http://localstack:4566"
    s3             = "http://localstack:4566"
    secretsmanager = "http://localstack:4566"
    ssm            = "http://localstack:4566"
  }
}


resource "aws_kms_key" "ucfs_etl_cmk" {
  description             = "UCFS ETL Master Key"
  deletion_window_in_days = 14
  is_enabled              = true
  enable_key_rotation     = true
  tags = merge(
    {
      "Name" = "UCFS ETL Master Key"
    },
  )
}

resource "aws_kms_alias" "ucfs_etl_cmk" {
  name          = "alias/ucfs_etl_cmk"
  target_key_id = aws_kms_key.ucfs_etl_cmk.key_id
}

resource "aws_s3_bucket" "dw_local_crl" {
  bucket = "dw-local-crl"
  acl    = "public-read"
}
