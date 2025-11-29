package com.example;

import jdk.jfr.Configuration;
import jdk.jfr.EventType;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import org.openjdk.jmh.annotations.*;
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
public class BlockingQueueBenchmark2 {

    private static final int LOOP_COUNT = 100_000;

    @State(Scope.Benchmark)
    public static class QueueState {
        @Param({"1000000"})
        int capacity;

        ArrayBlockingQueue<Payload> arrayQueue;
        LinkedBlockingQueue<Payload> linkedQueue;
        ConcurrentLinkedQueue<Payload> concurrentLinkedQueue;

        // --- JFR recording instance ---
        private jdk.jfr.Recording recording;

        @Setup(Level.Iteration)
        public void setup() {
            arrayQueue = new ArrayBlockingQueue<>(capacity);
            linkedQueue = new LinkedBlockingQueue<>(capacity);
            concurrentLinkedQueue = new ConcurrentLinkedQueue<>();
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
            state.arrayQueue.put(new Payload());
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
            state.linkedQueue.put(new Payload());
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

    // ============================
    // ConcurrentLinkedQueue
    // ============================

    @Group("concurrentLinked")
    @GroupThreads(10) // 2 producers
    @Benchmark
    public void concurrent_linked_put(QueueState state) {
//        for (int i = 0; i < LOOP_COUNT; i++) {
            state.concurrentLinkedQueue.offer(new Payload());
//        }
    }

    @Group("concurrentLinked")
    @GroupThreads(10) // 2 consumers
    @Benchmark
    public void concurrent_linked_poll(QueueState state, Blackhole bh) {
//        for (int i = 0; i < LOOP_COUNT; i++) {
            bh.consume(state.concurrentLinkedQueue.poll());
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
                "BlockingQueueBenchmark2.array",
                "-i", "5", "-wi", "5", "-f", "1"
        });
    }

    public static void runLinked() throws IOException {
        org.openjdk.jmh.Main.main(new String[] {
                "BlockingQueueBenchmark2.linked",
                "-i", "5", "-wi", "5", "-f", "1"
        });
    }

    public static void runConcurrentLinked() throws IOException {
        org.openjdk.jmh.Main.main(new String[] {
                "BlockingQueueBenchmark2.concurrentLinked",
                "-i", "5", "-wi", "5", "-f", "1"
        });
    }

    public static void main(String[] args) throws IOException, ParseException {
        BlockingQueueBenchmark2.runWithJfr("array.jfr", () -> {
            try {
                BlockingQueueBenchmark2.runArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        BlockingQueueBenchmark2.runWithJfr("linked.jfr", () -> {
            try {
                BlockingQueueBenchmark2.runLinked();
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


