package ucfs.claimant.consumer.repository.impl

import org.springframework.cache.annotation.Cacheable
<<<<<<< HEAD
import org.springframework.stereotype.Repository
=======
import org.springframework.stereotype.Component
>>>>>>> origin/master
import software.amazon.awssdk.services.ssm.SsmClient
import software.amazon.awssdk.services.ssm.model.GetParameterRequest
import ucfs.claimant.consumer.repository.SaltRepository

<<<<<<< HEAD
@Repository
=======
@Component
>>>>>>> origin/master
class SsmParameterStoreSaltRepository(private val ssmClient: SsmClient, private val saltParameterName: String):
    SaltRepository {

    @Cacheable("SALT_CACHE")
    override fun salt(): String =
        ssmClient.getParameter(getParameterRequest()).run {
            parameter().value()
        }

    private fun getParameterRequest(): GetParameterRequest =
        with(GetParameterRequest.builder()) {
            name(saltParameterName)
            build()
        }
}
