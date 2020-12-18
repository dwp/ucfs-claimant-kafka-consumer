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

.PHONY: git-hooks
git-hooks: ## Set up hooks in .githooks
	@git submodule update --init .githooks ; \
	git config core.hooksPath .githooks \

certificates: git-hooks ## generate self-signed certificates, keystores for local development.
	./generate-certificates.sh

rds:
	docker-compose up -d rds
	@{ \
		while ! docker logs rds 2>&1 | grep "^Version" | grep 3306; do \
			echo Waiting for rds.; \
			sleep 2; \
		done; \
		sleep 5; \
	}
	docker exec -i rds mysql --host=127.0.0.1 --user=root --password=password ucfs-claimant  < ./containers/rds/create_tables.sql

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

services: kafka rds localstack dks ## Bring up zookeeper, kafka, the database etc.

build: ## Build the container
	docker-compose build ucfs-claimant-kafka-consumer

up: build services ## Bring up the consumer in Docker with supporting services
	docker-compose up -d ucfs-claimant-kafka-consumer

build-tests:
	docker-compose build ucfs-claimant-kafka-consumer-tests

tests: up ## Run the integration tests without rebuilding the tests
	docker-compose up ucfs-claimant-kafka-consumer-tests

integration-all-github: certificates up tests ## Run the integration tests

integration-all-local: delete-topics integration-all-github ## Run the integration tests with fresh topics

integration-all-with-reset: destroy certificates build-tests up tests ## Run the integration tests on a fresh stack

down: ## Bring down all containers
	docker-compose down

destroy: down ## Bring down the containers and services then delete all volumes, networks
	docker network prune -f
	docker volume prune -f

delete-topic: ## Delete a topic
	docker exec -it kafka kafka-topics --zookeeper zookeeper:2181 --delete --if-exists --topic $(topic)

list-topics: ## List the topics
	docker exec -it kafka kafka-topics --zookeeper zookeeper:2181 --list

delete-topics: ## Clear the integration test queue.
	make delete-topic topic="db.core.claimant"
	make delete-topic topic="db.core.contract"
	make delete-topic topic="db.core.statement"
	make delete-topic topic="db.core.claimant.success"
	make delete-topic topic="db.core.contract.success"
	make delete-topic topic="db.core.statement.success"
	make delete-topic topic="dead.letter.queue"
	make list-topics

push-local-to-ecr: ## Push a temp version of the consumer to AWS MGMT-DEV ECR
	@{ \
		aws ecr get-login-password --region $(aws_default_region) --profile dataworks-management-dev | docker login --username AWS --password-stdin $(aws_mgmt_dev_account).dkr.ecr.$(aws_default_region).amazonaws.com; \
		docker tag ucfs-claimant-kafka-consumer $(aws_mgmt_dev_account).dkr.ecr.$(aws_default_region).amazonaws.com/ucfs-claimant-kafka-consumer:$(temp_image_tag); \
		docker push $(aws_mgmt_dev_account).dkr.ecr.$(aws_default_region).amazonaws.com/ucfs-claimant-kafka-consumer:$(temp_image_tag); \
	}

mysql_root: ## Get a root session on the  database.
	docker exec -it rds mysql --host=127.0.0.1 --user=root --password=password ucfs-claimant

mysql_user: ## Get a client session on the database.
	docker exec -it rds mysql --host=127.0.0.1 --user=claimantapi --password=password ucfs-claimant
