package com.example.orderbook;

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

public class OrderQueueVsLinkedListBenchmarkRunner {

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(OrderQueueVsLinkedListBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }
}
