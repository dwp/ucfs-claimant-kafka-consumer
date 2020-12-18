#!/usr/bin/env bash

init() {
  aws_local configure set aws_access_key_id access_key_id
  aws_local configure set aws_secret_access_key secret_access_key
  terraform init
}

create_crl_bucket() {
  make_bucket dw-local-crl
  aws_local s3api put-object --bucket dw-local-crl --key crl/
}

make_bucket() {
  local bucket_name=$1

  if ! aws_local s3 ls s3://$bucket_name 2>/dev/null; then
    echo Making $bucket_name
    aws_local s3 mb s3://$bucket_name
    aws_local s3api put-bucket-acl --bucket $bucket_name --acl public-read
  else
    echo Bucket \'$bucket_name\' exists.
  fi

}

#add_kms_key() {
#    local key_id=$(aws_local --output text kms create-key --query "KeyMetadata.KeyId")
#    echo Created key \'$key_id\'.
#
#    local existing_alias=$(aws_local --output text \
#                         kms list-aliases \
#                         --query "(Aliases[?AliasName == '$(kms_alias_name)'])[0].AliasName")
#
#    if  [[ -n $existing_alias ]] && \
#            [[ $existing_alias != 'null' ]] && \
#            [[ $existing_alias != 'None' ]]; then
#        aws_local kms delete-alias --alias-name $(kms_alias_name)
#        echo Deleted existing alias \'$existing_alias\'.
#    fi
#
#    aws_local kms create-alias --alias-name $(kms_alias_name) --target-key-id $key_id
#    echo Created alias \'$(kms_alias_name)\' for key \'$key_id\'.
#}

add_salt_parameter() {
  local existing=$(aws_local ssm get-parameters \
    --names $(salt_parameter_name) \
    --query 'Parameters[0].Value')
  if [[ -n $existing ]] && [[ $existing != "null" ]]; then
    echo Deleting existing parameter \'$(salt_parameter_name)\', value \'$existing\'
    aws_local ssm delete-parameter --name $(salt_parameter_name)
  fi

  echo Creating new parameter \'$(salt_parameter_name)\', \
    value \'$(salt_parameter_value)\'.

  aws_local ssm put-parameter --name $(salt_parameter_name) \
    --value $(salt_parameter_value)
}

salt_parameter_name() {
  echo /ucfs/claimant-api/nino/salt
}

salt_parameter_value() {
  echo SALT
}

aws_local() {
  aws --endpoint-url http://localstack:4566 --region=eu-west-2 "$@"
}
