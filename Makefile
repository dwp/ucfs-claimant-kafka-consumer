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

pushgateway:
	docker-compose up -d pushgateway

prometheus:
	docker-compose up -d prometheus

rds:
	docker-compose up -d rds
	@{ \
		while ! docker logs rds 2>&1 | grep "^Version" | grep 3306; do \
			echo Waiting for rds.; \
			sleep 2; \
		done; \
	}
	docker exec -i rds mysql --user=root --password=password ucfs-claimant  < ./containers/rds/create_tables.sql

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

services: kafka rds localstack dks pushgateway prometheus ## Bring up zookeeper, kafka, the database etc.

.PHONY: build
build: ## Build the container
	docker-compose build ucfs-claimant-kafka-consumer

stop-consumer:
	docker stop ucfs-claimant-kafka-consumer
	docker rm ucfs-claimant-kafka-consumer

start-consumer:
	docker-compose up -d ucfs-claimant-kafka-consumer

up: build services ## Bring up the consumer in Docker with supporting services
	docker-compose up -d ucfs-claimant-kafka-consumer

build-tests:
	docker-compose build ucfs-claimant-kafka-consumer-tests

tests: up ## Run the integration tests without rebuilding the tests
	docker-compose up ucfs-claimant-kafka-consumer-tests

down: ## Bring down all containers
	docker-compose down

destroy: down ## Bring down the containers and services then delete all volumes, networks
	docker network prune -f
	docker volume prune -f

list-topics: ## List the topics
	docker exec -it kafka kafka-topics --zookeeper zookeeper:2181 --list

list-consumer-groups: ## List the topics
	docker exec -it kafka kafka-consumer-groups --bootstrap-server kafka:9092  --list

reset-offset: ## Reset a topic's offsets
	docker exec -it kafka kafka-consumer-groups --bootstrap-server kafka:9092 --group ucfs-claimant-consumers --topic $(topic) --reset-offsets --to-earliest --execute

reset-offsets: ## Reset all offsets
	make reset-offset topic="db.core.claimant"
	make reset-offset topic="db.core.contract"
	make reset-offset topic="db.core.statement"
	make reset-offset topic="dead.letter.queue"

delete-topic: ## Delete a topic
	docker exec -it kafka kafka-topics --zookeeper zookeeper:2181 --delete --if-exists --topic $(topic)

delete-topics: ## Clear the integration test queue.
	make delete-topic topic="db.core.claimant"
	make delete-topic topic="db.core.contract"
	make delete-topic topic="db.core.statement"
	make delete-topic topic="dead.letter.queue"
	make list-topics

truncate-tables:
	docker exec -i rds mysql --user=claimantapi --password=password ucfs-claimant <<< \
		"truncate claimant; truncate contract; truncate statement"

push-local-to-ecr: ## Push a temp version of the consumer to AWS MGMT-DEV ECR
	@{ \
		aws ecr get-login-password --region $(aws_default_region) --profile dataworks-management-dev | docker login --username AWS --password-stdin $(aws_mgmt_dev_account).dkr.ecr.$(aws_default_region).amazonaws.com; \
		docker tag ucfs-claimant-kafka-consumer $(aws_mgmt_dev_account).dkr.ecr.$(aws_default_region).amazonaws.com/ucfs-claimant-kafka-consumer:$(temp_image_tag); \
		docker push $(aws_mgmt_dev_account).dkr.ecr.$(aws_default_region).amazonaws.com/ucfs-claimant-kafka-consumer:$(temp_image_tag); \
	}

rds_root: ## Get a root session on the database.
	docker exec -it rds mysql --user=root --password=password ucfs-claimant

rds_user: ## Get a client session on the database.
	docker exec -it rds mysql --user=claimantapi --password=password ucfs-claimant

restart-prometheus:
	docker stop prometheus pushgateway
	docker rm prometheus pushgateway
	docker-compose build prometheus
	docker-compose up -d prometheus pushgateway

clear-data: stop-consumer truncate-tables delete-topics restart-prometheus reset-offsets start-consumer

