#!/usr/bin/env bash

source ./environment.sh

main() {
  init
  create_crl_bucket
  add_salt_parameter
#  add_kms_key
}

main
terraform init
terraform apply -auto-approve
