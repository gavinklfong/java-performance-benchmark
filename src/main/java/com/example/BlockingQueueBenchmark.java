package com.example;

import org.openjdk.jmh.annotations.*;
import java.util.concurrent.*;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class BlockingQueueBenchmark {

    // ============================================================
    //                         Queue State
    // ============================================================
    @State(Scope.Group)
    public static class QueueState {

        @Param({"array", "linked"})
//        @Param({"array"})
        public String type;

        @Param({"1024"})   // capacity for ArrayBlockingQueue
        public int capacity;

        BlockingQueue<Integer> queue;

        @Setup(Level.Trial)
        public void setup() {
            switch (type) {
                case "array":
                    queue = new ArrayBlockingQueue<>(capacity);
                    break;
                case "linked":
                    queue = new LinkedBlockingQueue<>(capacity);
                    break;
            }
        }
    }

    // ============================================================
    //                          Producer
    // ============================================================
    @Group("queue")
    @GroupThreads(4)  // four producers
    @Benchmark
    public void produce(QueueState s) throws Exception {
        s.queue.offer(1, 1, TimeUnit.SECONDS); // uses blocking put to avoid dropping
    }

    // ============================================================
    //                          Consumer
    // ============================================================
    @Group("queue")
    @GroupThreads(4)  // four consumers
    @Benchmark
    public Integer consume(QueueState s) throws Exception {
        return s.queue.poll(1, TimeUnit.SECONDS);
    }
}
