package com.example.orderbook;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class OrderPoolBenchmarkRunner {

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(OrderPoolBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }
}
