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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class ArrayListBenchmark {

    @Param({"10000000"})
    public int size;

    private List<Integer> arrayList;
    private Random random;

    @Setup(Level.Trial)
    public void setup() {
        arrayList = new ArrayList<>(size);
        random = new Random(42);

        for (int i = 0; i < size; i++) {
            arrayList.add(i);
        }
    }

    // =============== ADDING ====================

    @Benchmark
    public List<Integer> arrayListAdd() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < size; i++) list.add(i);
        return list;
    }

    // =============== RANDOM ACCESS ====================

    @Benchmark
    public int arrayListRandomAccess() {
        return arrayList.get(random.nextInt(size));
    }

    // =============== ITERATION ====================

    @Benchmark
    public long arrayListIteration() {
        long sum = 0;
        for (int value : arrayList) sum += value;
        return sum;
    }
}

