package com.example.orderbook;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(1)
public class NextIndexBenchmark {

    @State(Scope.Thread)
    public static class QueueState {
//        @Param({"128", "512", "1024", "4096"})
        @Param({"4096"})
        int capacity; // For AND method, capacity must be power of 2

        int tail = 0;
        int iterations = 1_000_000;
    }

    /* ======================
       Bitwise AND (power-of-2) method
       ====================== */
    @Benchmark
    public void nextIndexAnd(QueueState s, Blackhole bh) {
        int tail = s.tail;
        int capacity = s.capacity;

        for (int i = 0; i < s.iterations; i++) {
            tail = (tail + 1) & (capacity - 1);
            bh.consume(tail);
        }
    }

    /* ======================
       Modulo method (general)
       ====================== */
    @Benchmark
    public void nextIndexMod(QueueState s, Blackhole bh) {
        int tail = s.tail;
        int capacity = s.capacity;

        for (int i = 0; i < s.iterations; i++) {
            tail = (tail + 1) % capacity;
            bh.consume(tail);
        }
    }
}

