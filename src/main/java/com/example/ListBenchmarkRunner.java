package com.example;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class ListBenchmarkRunner {
    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(ListBenchmark.class.getSimpleName())
                .forks(1)
                .warmupIterations(0)
                .measurementIterations(1)
                .build();

        new Runner(opt).run();
    }
}

