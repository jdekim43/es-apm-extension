package kr.jadekim.apm.es

import co.elastic.apm.api.ElasticApm
import co.elastic.apm.api.Span
import co.elastic.apm.api.Transaction
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

class EsApmTransactionContext(
        val transaction: Transaction
) : AbstractCoroutineContextElement(Key) {

    companion object Key : CoroutineContext.Key<EsApmTransactionContext>
}

suspend inline fun esTransaction() = coroutineContext[EsApmTransactionContext]?.transaction
        ?: ElasticApm.currentTransaction()

suspend inline fun <T> inEsTransaction(withEnd: Boolean = false, body: Transaction.() -> T): T {
    val transaction = esTransaction()

    return try {
        transaction.body()
    } catch (e: Exception) {
        transaction.captureException(e)

        throw e
    } finally {
        if (withEnd) {
            transaction.end()
        }
    }
}

suspend inline fun <T> newEsSpan(
        type: String,
        subtype: String? = null,
        action: String? = null,
        name: String? = null,
        body: Span.() -> T
): T = esTransaction().newSpan(type, subtype, action, name, body)