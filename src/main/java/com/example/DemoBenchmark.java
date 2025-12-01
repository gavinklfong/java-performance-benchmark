package com.example;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;

//@BenchmarkMode(Mode.AverageTime)
//@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class DemoBenchmark {

    @Param({"100"})
    private int loopCount;

    private DemoOperation demoOperation;

    @Setup(Level.Trial)
    public void setup() {
        demoOperation = new DemoOperation(loopCount);
    }

    @Benchmark
    public void runCalculateRandomSum() {
        demoOperation.calculateRandomSum();
    }

}
