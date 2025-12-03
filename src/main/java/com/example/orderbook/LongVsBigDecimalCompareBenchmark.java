package com.example.orderbook;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class LongVsBigDecimalCompareBenchmark {

    @Param({"100", "10000", "10000000"})
    long longA;

    @Param({"101", "9999", "20000000"})
    long longB;

    BigDecimal bdA;
    BigDecimal bdB;

    @Setup
    public void setup() {
        bdA = BigDecimal.valueOf(longA);
        bdB = BigDecimal.valueOf(longB);
    }

    /* ============================
       LONG COMPARISON
       ============================ */

    @Benchmark
    public void compareLongs(Blackhole bh) {
        bh.consume(Long.compare(longA, longB));
    }

    @Benchmark
    public void compareLongGreater(Blackhole bh) {
        bh.consume(longA > longB);
    }

    @Benchmark
    public void compareLongLess(Blackhole bh) {
        bh.consume(longA < longB);
    }

    /* ============================
       BIGDECIMAL COMPARISON
       ============================ */

    @Benchmark
    public void compareBigDecimal(Blackhole bh) {
        bh.consume(bdA.compareTo(bdB));
    }

    @Benchmark
    public void compareBigDecimalGreater(Blackhole bh) {
        bh.consume(bdA.compareTo(bdB) > 0);
    }

    @Benchmark
    public void compareBigDecimalLess(Blackhole bh) {
        bh.consume(bdA.compareTo(bdB) < 0);
    }
}
