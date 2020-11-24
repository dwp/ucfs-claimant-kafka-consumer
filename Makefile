SHELL:=bash

aws_mgmt_dev_account="NOT_SET"
aws_default_region="NOT_SET"
temp_image_tag="my-test"

default: help

.PHONY: help
help:
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

.PHONY: bootstrap
bootstrap: ## Bootstrap local environment for first use
	@make git-hooks
	pip3 install --user Jinja2 PyYAML boto3
	@{ \
		export AWS_PROFILE=default; \
		export AWS_REGION=eu-west-2; \
		python3 bootstrap_terraform.py; \
	}
	terraform fmt -recursive

.PHONY: git-hooks
git-hooks: ## Set up hooks in .githooks
	@git submodule update --init .githooks ; \
	git config core.hooksPath .githooks \

certificates: ## generate self-signed certificates, keystores for local development.
	./generate-certificates.sh

localstack: ## bring up localstack container and wait for it to be ready
	docker-compose up -d localstack
	@{ \
		while ! docker logs localstack 2> /dev/null | grep -q "^Ready\." ; do \
			echo Waiting for localstack.; \
			sleep 2; \
		done; \
	}
	docker-compose up localstack-init

kafka:
	docker-compose up -d zookeeper kafka

dks:
	docker-compose up -d dks
	@{ \
		while ! docker exec dks cat logs/dks.out 2>/dev/null | grep -q "Started DataKeyServiceApplication"; do \
			echo Waiting for dks.; \
			sleep 2; \
		done; \
	}

services: kafka localstack dks ## Bring up zookeeper, kafka

build: ## Build the container
	docker-compose build ucfs-claimant-kafka-consumer

up: build services ## Bring up the consumer in Docker with supporting services
	docker-compose up --build -d ucfs-claimant-kafka-consumer

tests: up ## Run the integration tests
	docker-compose up ucfs-claimant-kafka-consumer-tests

integration-all: certificates up tests ## Run the integration tests

down: ## Bring down all containers
	docker-compose down

destroy: down ## Bring down the containers and services then delete all volumes, networks
	docker network prune -f
	docker volume prune -f

delete-topic: ## Run an arbitrary command in the kafka server
	docker exec -it kafka kafka-topics --zookeeper zookeeper:2181 --delete --if-exists --topic $(topic)

list-topics: ## List the topics on the local broker.
	docker exec -it kafka kafka-topics --zookeeper zookeeper:2181 --list

delete-topics: ## Clear the integration test queue.
	make delete-topic topic="db.database.collection1"
	make delete-topic topic="db.database.collection2"
	make delete-topic topic="db.database.collection3"
	make delete-topic topic="db.database.collection1.success"
	make delete-topic topic="db.database.collection2.success"
	make delete-topic topic="db.database.collection3.success"
	make delete-topic topic="dead.letter.queue"
	make list-topics

push-local-to-ecr: ## Push a temp version of the consumer to AWS MGMT-DEV ECR
	@{ \
		aws ecr get-login-password --region $(aws_default_region) --profile dataworks-management-dev | docker login --username AWS --password-stdin $(aws_mgmt_dev_account).dkr.ecr.$(aws_default_region).amazonaws.com; \
		docker tag ucfs-claimant-kafka-consumer $(aws_mgmt_dev_account).dkr.ecr.$(aws_default_region).amazonaws.com/ucfs-claimant-kafka-consumer:$(temp_image_tag); \
		docker push $(aws_mgmt_dev_account).dkr.ecr.$(aws_default_region).amazonaws.com/ucfs-claimant-kafka-consumer:$(temp_image_tag); \
	}
