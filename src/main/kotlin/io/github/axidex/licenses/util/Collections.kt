package io.github.axidex.licenses.util

import java.util.concurrent.Executors

/**
 * @param transform
 * @return list of results in the same order as the original collection
 */
internal fun <T, R> Collection<T>.pmap(transform: (T) -> R): List<R> {
    if (isEmpty()) {
        return emptyList()
    }
    val threads = minOf(size, Runtime.getRuntime().availableProcessors() * 2)
    val executor = Executors.newFixedThreadPool(threads)
    return try {
        map { executor.submit<R> { transform(it) } }.map { it.get() }
    } finally {
        executor.shutdown()
    }
}
