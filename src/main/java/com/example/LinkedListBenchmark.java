package com.example;

import org.openjdk.jmh.annotations.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class LinkedListBenchmark {

    @Param({"10000000"})
    public int size;

    private List<Integer> linkedList;
    private Random random;

    @Setup(Level.Trial)
    public void setup() {
        linkedList = new LinkedList<>();
        random = new Random(42);

        for (int i = 0; i < size; i++) {
            linkedList.add(i);
        }
    }

    // =============== ADDING ====================
    @Benchmark
    public List<Integer> linkedListAdd() {
        List<Integer> list = new LinkedList<>();
        for (int i = 0; i < size; i++) list.add(i);
        return list;
    }

    // =============== RANDOM ACCESS ====================

    @Benchmark
    public int linkedListRandomAccess() {
        return linkedList.get(random.nextInt(size));
    }

    // =============== ITERATION ====================

    @Benchmark
    public long linkedListIteration() {
        long sum = 0;
        for (int value : linkedList) sum += value;
        return sum;
    }
}

