package com.example;

import org.openjdk.jmh.annotations.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class LinkedListBenchmark {

    @Param({"100"})
    public int size;

    private List<Integer> linkedList;
    private RandomGenerator randomGenerator;

    @Setup(Level.Trial)
    public void setup() {
        randomGenerator = RandomGenerator.getDefault();
        linkedList = new LinkedList<>();

        for (int i = 0; i < size; i++) {
            linkedList.add(i);
        }
    }

    // =============== ADDING ====================
    @Benchmark
    public Integer linkedListAppend() {
        int num = randomGenerator.nextInt(100, 1000);
        linkedList.add(num);
        return num;
    }

    @Benchmark
    public Integer linkedListInsert() {
        int num = randomGenerator.nextInt(100, 1000);
        linkedList.add(randomGenerator.nextInt(0, linkedList.size() - 1), num);
        return num;
    }

    @Benchmark
    public Integer linkedListTraverseAndInsert() {
        int num = randomGenerator.nextInt(100, 1000);
        int position = randomGenerator.nextInt(linkedList.size());

        ListIterator<Integer> it = linkedList.listIterator();
        for (int i = 0; i < position + 1; i++) {
            it.next();
        }
        it.add(num);

        return num;
    }

    // =============== REMOVE ====================
    @Benchmark
    public int linkedListRemoveFirst() {
        return linkedList.removeFirst();
    }

    @Benchmark
    public int linkedListRemove() {
        int position = randomGenerator.nextInt(linkedList.size());
        return linkedList.remove(position);
    }

    // =============== RANDOM ACCESS ====================
    @Benchmark
    public int linkedListRandomAccess() {
        return linkedList.get(randomGenerator.nextInt(size));
    }

    // =============== ITERATION ====================
    @Benchmark
    public long linkedListIteration() {
        long sum = 0;
        for (int value : linkedList) sum += value;
        return sum;
    }
}

