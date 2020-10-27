SHELL:=bash

aws_profile=default
aws_region=eu-west-2

default: help

.PHONY: help
help:
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

.PHONY: bootstrap
bootstrap: ## Bootstrap local environment for first use
	@make git-hooks
	pip3 install --user Jinja2 PyYAML boto3
	@{ \
		export AWS_PROFILE=$(aws_profile); \
		export AWS_REGION=$(aws_region); \
		python3 bootstrap_terraform.py; \
	}
	terraform fmt -recursive

.PHONY: git-hooks
git-hooks: ## Set up hooks in .githooks
	@git submodule update --init .githooks ; \
	git config core.hooksPath .githooks \

.PHONY: services
services: ## Bring up UCKC in Docker with supporting services
	docker-compose up -d zookeeper kafka uckc

.PHONY: up
up: ## Bring up UCKC in Docker with supporting services
	docker-compose up --build -d

.PHONY: down
down: ## Bring down the UCKC Docker container and support services
	docker-compose down

.PHONY: destroy
destroy: down ## Bring down the UCKC Docker container and services then delete all volumes
	docker network prune -f
	docker volume prune -f