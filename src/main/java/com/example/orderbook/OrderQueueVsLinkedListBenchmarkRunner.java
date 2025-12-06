package com.example.orderbook;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;


public class OrderQueueVsLinkedListBenchmarkRunner {

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(OrderQueueVsLinkedListBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }
}
