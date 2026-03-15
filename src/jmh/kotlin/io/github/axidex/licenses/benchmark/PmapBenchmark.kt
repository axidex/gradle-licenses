package io.github.axidex.licenses.benchmark

import io.github.axidex.licenses.util.pmap
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
@State(Scope.Benchmark)
open class PmapBenchmark {

    private val items = List(50) { it }

    @Benchmark
    fun sequential(): List<Int> =
        items.map { Thread.sleep(10); it }

    @Benchmark
    fun parallel(): List<Int> =
        items.pmap { Thread.sleep(10); it }
}