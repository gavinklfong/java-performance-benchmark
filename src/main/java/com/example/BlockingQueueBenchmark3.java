package com.example;

import jdk.jfr.Configuration;
import jdk.jfr.EventType;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark to run put() and take() in true parallel using dedicated producer and consumer threads.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class BlockingQueueBenchmark3 {

    @State(Scope.Benchmark)
    public static class QueueState {
        @Param({"1000000"})
        int capacity;

        ArrayBlockingQueue<Integer> arrayQueue;
        LinkedBlockingQueue<Integer> linkedQueue;

        // --- JFR recording instance ---
        private Recording recording;

        @Setup(Level.Iteration)
        public void setup() {
            arrayQueue = new ArrayBlockingQueue<>(capacity);
            linkedQueue = new LinkedBlockingQueue<>(capacity);
        }
    }

    // Large object to remove trivial alloc cost advantage of ABQ
    static class Payload {
        byte[] data = new byte[4096]; // 4 KB
    }

    // ============================
    // ArrayBlockingQueue
    // ============================

    @Group("array")
    @GroupThreads(1) // 2 producers
    @Benchmark
    public void array_put(QueueState state) throws InterruptedException {
//        for (int i = 0; i < LOOP_COUNT; i++) {
            state.arrayQueue.put(1);
//        }
    }

    @Group("array")
    @GroupThreads(1) // 2 consumers
    @Benchmark
    public void array_take(QueueState state, Blackhole bh) throws InterruptedException {
//        for (int i = 0; i < LOOP_COUNT; i++) {
            bh.consume(state.arrayQueue.take());
//        }
    }

    // ============================
    // LinkedBlockingQueue
    // ============================

    @Group("linked")
    @GroupThreads(1) // 2 producers
    @Benchmark
    public void linked_put(QueueState state) throws InterruptedException {
//        for (int i = 0; i < LOOP_COUNT; i++) {
            state.linkedQueue.put(1);
//        }
    }

    @Group("linked")
    @GroupThreads(1) // 2 consumers
    @Benchmark
    public void linked_take(QueueState state, Blackhole bh) throws InterruptedException {
//        for (int i = 0; i < LOOP_COUNT; i++) {
            bh.consume(state.linkedQueue.take());
//        }
    }
    // --- Programmatic JFR recording harness ---
    public static void runWithJfr(String fileName, Runnable benchmarkLogic) throws IOException, ParseException {
        Configuration config = Configuration.getConfiguration("profile");

        try (Recording recording = new Recording(config)) {
//            recording.setToDisk(true);
            for (EventType eventType : FlightRecorder.getFlightRecorder().getEventTypes()) {
                recording.enable(eventType.getName());
            }
            recording.start();

            benchmarkLogic.run();

            recording.stop();
            recording.dump(Paths.get(fileName));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void runArray() throws IOException {
        org.openjdk.jmh.Main.main(new String[] {
                "BlockingQueueBenchmark3.array",
                "-i", "5", "-wi", "5", "-f", "1"
        });
    }

    public static void runLinked() throws IOException {
        org.openjdk.jmh.Main.main(new String[] {
                "BlockingQueueBenchmark3.linked",
                "-i", "5", "-wi", "5", "-f", "1"
        });
    }

    public static void main(String[] args) throws IOException, ParseException {
        BlockingQueueBenchmark3.runWithJfr("array_int.jfr", () -> {
            try {
                BlockingQueueBenchmark3.runArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        BlockingQueueBenchmark3.runWithJfr("linked_int.jfr", () -> {
            try {
                BlockingQueueBenchmark3.runLinked();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
//        BlockingQueueBenchmark2.runWithJfr("concurrentLinked.jfr", () -> {
//            try {
//                BlockingQueueBenchmark2.runConcurrentLinked();
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        });
    }
}


