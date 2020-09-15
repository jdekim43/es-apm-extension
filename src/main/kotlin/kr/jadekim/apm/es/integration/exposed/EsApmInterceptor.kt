package kr.jadekim.apm.es.integration.exposed

import co.elastic.apm.api.Span
import kr.jadekim.apm.es.esTransaction
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.statements.StatementInterceptor

suspend fun Transaction.registerEsApm() {
    registerInterceptor(traceEsApm())
}

suspend fun traceEsApm(): StatementInterceptor {
    val apm = esTransaction()

    return object : StatementInterceptor {

        private var span: Span? = null

        override fun beforeExecution(transaction: Transaction, context: StatementContext) {
            span = apm.startSpan("db", transaction.db.vendor, context.statement.type.name)
                .setName(context.statement.prepareSQL(transaction))
                .addLabel("url", transaction.db.url)
        }

        override fun afterCommit() {
            span?.addLabel("result", "commit")?.end()
        }

        override fun afterRollback() {
            span?.addLabel("result", "rollback")?.end()
        }
    }
}