package kr.jadekim.apm.es.integration.exposed

import kr.jadekim.db.exposed.CrudDB
import kr.jadekim.db.exposed.ReadDB
import org.jetbrains.exposed.sql.Transaction

open class ESReadDB(
        private val delegate: ReadDB
) {

    suspend fun <T> read(statement: suspend Transaction.() -> T): T = delegate.read {
        registerEsApm()
        statement()
    }
}

open class ESCrudDB(
        private val delegate: CrudDB
) : ESReadDB(delegate) {

    suspend fun <T> execute(statement: suspend Transaction.() -> T): T = delegate.execute {
        registerEsApm()
        statement()
    }
}