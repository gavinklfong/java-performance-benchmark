package com.example.orderbook;

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

public class LongVsBigDecimalCompareBenchmarkRunner {
    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(LongVsBigDecimalCompareBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }
}

