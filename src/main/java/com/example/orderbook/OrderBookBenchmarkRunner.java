package com.example.orderbook;

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

public class OrderBookBenchmarkRunner {
    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(OrderBookBenchmark.class.getSimpleName())
                .mode(Mode.Throughput)
                .timeUnit(TimeUnit.NANOSECONDS)
                .forks(1)
                .warmupIterations(2)
                .measurementIterations(3)
                .build();

        new Runner(opt).run();
    }
}

