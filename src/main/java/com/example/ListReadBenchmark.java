package com.example;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 8, time = 1, timeUnit = TimeUnit.SECONDS)
//@Fork(value = 3, jvmArgsAppend = {
//        "-Xms2g",
//        "-Xmx2g",
//        "-XX:+UnlockDiagnosticVMOptions",
//        "-XX:+PrintInlining"
//})
@State(Scope.Thread)
@Threads(4)
public class ListReadBenchmark {

    @Param({"1000", "10000", "100000"})
    int size;

    List<Integer> arrayList;
    List<Integer> linkedList;

    @Setup(Level.Trial)
    public void setup() {
        arrayList = new ArrayList<>(size);
        linkedList = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            arrayList.add(i);
            linkedList.add(i);
        }
    }

    @Benchmark
    public void arrayListRead(Blackhole bh) {
        // read all elements
        for (int i = 0; i < size; i++) {
            bh.consume(arrayList.get(i));
        }
    }

    @Benchmark
    public void linkedListRead(Blackhole bh) {
        for (int i = 0; i < size; i++) {
            bh.consume(linkedList.get(i)); // O(n) each
        }
    }
}
