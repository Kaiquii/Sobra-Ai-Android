package com.example.appfinanceiro.core.designsystem.components

import com.example.appfinanceiro.core.data.NetworkFailureKind

data class LoadFailure<T>(val source: T, val label: String, val error: String?,
    val networkFailure: NetworkFailureKind?, val cached: Boolean, val retrying: Boolean)
data class LoadNotice<T>(val sources: Set<T>, val message: String, val retrying: Boolean)

/** Shared by every screen with independent reads. API failures are never guessed from text. */
fun <T> loadNotices(reads: List<LoadFailure<T>>): List<LoadNotice<T>> {
    val failures = reads.filter { it.error != null }
    val handled = mutableSetOf<T>()
    return buildList {
        failures.forEach { failure ->
            if (failure.source !in handled) {
                val group = if (failure.networkFailure == null) listOf(failure)
                    else failures.filter { it.networkFailure == failure.networkFailure }
                val sources = group.map { it.source }.toSet()
                handled.addAll(sources)
                val message = when (failure.networkFailure) {
                    NetworkFailureKind.Connection -> if (group.size > 1) "Falha de conexão com o servidor."
                        else "${failure.label}: falha de conexão com o servidor."
                    NetworkFailureKind.Timeout -> if (group.size > 1) "O servidor demorou para responder."
                        else "${failure.label}: o servidor demorou para responder."
                    null -> "${failure.label}: ${failure.error}"
                } + if (group.any { it.cached }) " Exibindo dados salvos." else ""
                add(LoadNotice(sources, message, group.any { it.retrying }))
            }
        }
    }
}
