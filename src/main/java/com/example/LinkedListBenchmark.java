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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class LinkedListBenchmark {


    // -----------------------------
    // State for add() benchmarks
    // -----------------------------
    @State(Scope.Thread)
    public static class AddState {
        @Param({"100"})
        int size;

        LinkedList<Integer> list;

        @Setup(Level.Invocation)
        public void setup() {
            list = new LinkedList<>();
            for (int i = 0; i < size; i++) {
                list.add(i);
            }
        }
    }

    // -----------------------------
    // State for get() + iteration
    // -----------------------------
    @State(Scope.Thread)
    public static class ReadState {
        @Param({"100"})
        int size;

        LinkedList<Integer> list;

        @Setup(Level.Trial)
        public void setup() {
            list = new LinkedList<>();
            for (int i = 0; i < size; i++) {
                list.add(i);
            }
        }
    }

    // =============================
    //         add() tests
    // =============================
    @Benchmark
    public boolean add_end(AddState state) {
        return state.list.add(12345);
    }

    @Benchmark
    public void add_middle(AddState state) {
        state.list.add(state.size / 2, 99999);
    }

    // =============================
    //         get() tests
    // =============================
    @Benchmark
    public Integer get_first(ReadState state) {
        return state.list.getFirst();
    }

    @Benchmark
    public Integer get_middle(ReadState state) {
        return state.list.get(state.size / 2);
    }

    @Benchmark
    public Integer get_last(ReadState state) {
        return state.list.getLast();
    }

    // =============================
    //        iteration tests
    // =============================
    @Benchmark
    public int iterate_for_each(ReadState state) {
        int sum = 0;
        for (int v : state.list) {
            sum += v;
        }
        return sum;
    }

    @Benchmark
    public int iterate_iterator(ReadState state) {
        int sum = 0;
        Iterator<Integer> it = state.list.iterator();
        while (it.hasNext()) {
            sum += it.next();
        }
        return sum;
    }
}

