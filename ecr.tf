resource "aws_ecr_repository" "ucfs-claimant-kafka-consumer" {
  name = "ucfs-claimant-kafka-consumer"
  tags = merge(
    local.common_tags,
    { DockerHub : "dwpdigital/ucfs-claimant-kafka-consumer" }
  )
}

resource "aws_ecr_repository_policy" "ucfs-claimant-kafka-consumer" {
  repository = aws_ecr_repository.ucfs-claimant-kafka-consumer.name
  policy     = data.terraform_remote_state.management.outputs.ecr_iam_policy_document
}

output "ecr_example_url" {
  value = aws_ecr_repository.ucfs-claimant-kafka-consumer.repository_url
}
