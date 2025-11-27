package com.example;

import org.openjdk.jmh.annotations.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class ListBenchmark {

    @Param({"100"})
    public int size;

    private List<Integer> arrayList;
    private List<Integer> linkedList;
    private Random random;

    @Setup(Level.Trial)
    public void setup() {
        arrayList = new ArrayList<>(size);
        linkedList = new LinkedList<>();
        random = new Random(42);

        for (int i = 0; i < size; i++) {
            arrayList.add(i);
            linkedList.add(i);
        }
    }

    // =============== ADDING ====================

    @Benchmark
    public List<Integer> arrayListAdd() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < size; i++) list.add(i);
        return list;
    }

    @Benchmark
    public List<Integer> linkedListAdd() {
        List<Integer> list = new LinkedList<>();
        for (int i = 0; i < size; i++) list.add(i);
        return list;
    }

    // =============== RANDOM ACCESS ====================

    @Benchmark
    public int arrayListRandomAccess() {
        return arrayList.get(random.nextInt(size));
    }

    @Benchmark
    public int linkedListRandomAccess() {
        return linkedList.get(random.nextInt(size));
    }

    // =============== ITERATION ====================

    @Benchmark
    public long arrayListIteration() {
        long sum = 0;
        for (int value : arrayList) sum += value;
        return sum;
    }

    @Benchmark
    public long linkedListIteration() {
        long sum = 0;
        for (int value : linkedList) sum += value;
        return sum;
    }
}

