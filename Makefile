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
services: ## Bring up zookeeper, kafka
	docker-compose up -d zookeeper kafka

.PHONY: up
up: services ## Bring up the consumer in Docker with supporting services
	docker-compose up --build -d ucfs-claimant-kafka-consumer

tests: up ## Run the integration tests
	docker-compose up ucfs-claimant-kafka-consumer-tests

.PHONY: down
down: ## Bring down all containers
	docker-compose down

.PHONY: destroy
destroy: down ## Bring down the containers and services then delete all volumes, networks
	docker network prune -f
	docker volume prune -f

clear-queue: ## Clear the integration test queue.
	docker exec -it kafka kafka-topics --delete --topic db.database.collection  --zookeeper zookeeper:2181

list-queues: ## List the queues on the local broker.
	docker exec -it kafka kafka-topics --list --zookeeper zookeeper:2181
