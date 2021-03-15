package ucfs.claimant.consumer.repository.impl

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TestContext {
    @Bean
    fun saltParameterName() = "SALT_PARAMETER_NAME"

    @Bean
    fun cmkAlias() = "CMK_ALIAS"

    @Bean
    fun dataKeySpec() = "DATA_KEY_SPEC"
}
