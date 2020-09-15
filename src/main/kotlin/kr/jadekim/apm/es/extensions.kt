package kr.jadekim.apm.es

import co.elastic.apm.api.Span
import co.elastic.apm.api.Transaction

inline fun <T> Transaction.use(body: Transaction.() -> T): T = try {
    body()
} catch (e: Exception) {
    captureException(e)

    throw e
} finally {
    end()
}

inline fun <T> Transaction.newSpan(type: String, subtype: String? = null, action: String? = null, name: String? = null, body: Span.() -> T): T {
    val span = startSpan(type, subtype, action)

    if (name != null) {
        span.setName(name)
    }

    return span.use(body)
}

inline fun <T> Span.use(body: Span.() -> T): T = try {
    body()
} catch (e: Exception) {
    captureException(e)

    throw e
} finally {
    end()
}