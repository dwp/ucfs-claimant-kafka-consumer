package ucfs.claimant.consumer.service

interface PushGatewayService {
    fun pushMetrics()
    fun pushFinalMetrics()
    fun deleteMetrics()
}
