package io.github.axidex.licenses.util

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

internal fun <T, R> Collection<T>.pmapExecutor(transform: (T) -> R): List<R> {
    if (isEmpty()) return emptyList()
    val threads = minOf(size, Runtime.getRuntime().availableProcessors() * 2)
    val executor = Executors.newFixedThreadPool(threads)
    return try {
        map { executor.submit<R> { transform(it) } }.map { it.get() }
    } finally {
        executor.shutdown()
    }
}

internal fun <T, R> Collection<T>.pmap(transform: suspend (T) -> R): List<R> {
    if (isEmpty()) return emptyList()
    val threads = minOf(size, Runtime.getRuntime().availableProcessors() * 2)
    val executor = Executors.newFixedThreadPool(threads)
    return try {
        runBlocking(executor.asCoroutineDispatcher()) {
            coroutineScope {
                map { async { transform(it) } }.awaitAll()
            }
        }
    } finally {
        executor.shutdown()
    }
}
