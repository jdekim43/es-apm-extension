package kr.jadekim.apm.es.integration.ktor

import co.elastic.apm.api.ElasticApm
import co.elastic.apm.api.Transaction
import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.request.header
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.util.AttributeKey
import kotlinx.coroutines.withContext
import kr.jadekim.apm.es.EsApmTransactionContext
import kr.jadekim.apm.es.esTransaction
import kr.jadekim.logger.context.CoroutineLogContext
import kr.jadekim.server.ktor.feature.PATH

class EsApmIntegration private constructor(private val withRemoteParent: Boolean) {

    class Configuration {
        var withRemoteParent: Boolean = false
    }

    companion object Feature : ApplicationFeature<Application, Configuration, EsApmIntegration> {

        override val key: AttributeKey<EsApmIntegration> = AttributeKey("EsApmIntegration")

        override fun install(
                pipeline: Application,
                configure: Configuration.() -> Unit
        ): EsApmIntegration {
            val configuration = Configuration().apply(configure)
            val feature = EsApmIntegration(configuration.withRemoteParent)

            pipeline.intercept(ApplicationCallPipeline.Monitoring) {
                if (context.request.path() in listOf("/liveness", "/readiness", "/health")) {
                    return@intercept
                }

                val transaction = if (feature.withRemoteParent) {
                    ElasticApm.startTransactionWithRemoteParent { context.request.header(it) }
                } else {
                    ElasticApm.startTransaction()
                }

                coroutineContext[CoroutineLogContext]?.set("trace.id", transaction.traceId)

                context.attributes.put(APM_TRANSACTION_CONTEXT, transaction)

                try {
                    withContext(EsApmTransactionContext(transaction)) {
                        proceed()
                    }
                } catch (e: Exception) {
                    transaction.captureException(e)

                    throw e
                } finally {

                    transaction.end()
                }
            }

            pipeline.intercept(ApplicationCallPipeline.Call) {
                try {
                    proceed()
                } finally {
                    val transactionName = context.attributes.getOrNull(PATH)
                            ?: "${context.request.path()}/(method:${context.request.httpMethod.value})"
                    esTransaction().setName(transactionName)
                }
            }

            return feature
        }
    }
}

val APM_TRANSACTION_CONTEXT = AttributeKey<Transaction>("apm.transaction-context")