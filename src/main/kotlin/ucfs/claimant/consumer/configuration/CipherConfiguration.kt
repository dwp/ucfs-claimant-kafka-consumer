package ucfs.claimant.consumer.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.security.SecureRandom

@Configuration
class CipherConfiguration {

    @Bean
    @Profile("strongRng")
    fun strongRandom(): SecureRandom = SecureRandom.getInstanceStrong()


    @Bean
    @Profile("!strongRng")
    fun weakRandom(): SecureRandom = SecureRandom.getInstance("SHA1PRNG")
}
