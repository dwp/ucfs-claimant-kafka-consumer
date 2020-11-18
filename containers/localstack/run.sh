#!/usr/bin/env bash

source ./environment.sh

main() {
  init
  create_crl_bucket
}

main
