package com.example;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
@State(Scope.Group)
public class BlockingQueueBenchmark {

    @Param({"1000000"}) // capacity: change if needed
    int capacity;

    ArrayBlockingQueue<Integer> arrayQueue;
    LinkedBlockingQueue<Integer> linkedQueue;

    @Setup(Level.Iteration)
    public void setup() {
        arrayQueue = new ArrayBlockingQueue<>(capacity);
        linkedQueue = new LinkedBlockingQueue<>(capacity);
    }

    // ----------------------------
    // ArrayBlockingQueue benchmark
    // ----------------------------

    @Group("array")
    @GroupThreads(4)
    @Benchmark
    public void array_put() throws InterruptedException {
        arrayQueue.put(1);
    }

    @Group("array")
    @GroupThreads(4)
    @Benchmark
    public Integer array_take(Blackhole bh) throws InterruptedException {
        Integer v = arrayQueue.take();
        bh.consume(v);
        return v;
    }

    // ----------------------------
    // LinkedBlockingQueue benchmark
    // ----------------------------

    @Group("linked")
    @GroupThreads(4)
    @Benchmark
    public void linked_put() throws InterruptedException {
        linkedQueue.put(1);
    }

    @Group("linked")
    @GroupThreads(4)
    @Benchmark
    public Integer linked_take(Blackhole bh) throws InterruptedException {
        Integer v = linkedQueue.take();
        bh.consume(v);
        return v;
    }
}
