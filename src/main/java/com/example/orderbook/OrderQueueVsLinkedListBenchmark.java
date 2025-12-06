package com.example.orderbook;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;


@Warmup(iterations = 3)
@Measurement(iterations = 5)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 1, jvmArgs = {
        "-Xms1g",
        "-Xmx1g"
})
public class OrderQueueVsLinkedListBenchmark {

    /* ======================
     * OrderLong model
     * ====================== */
    public static final class OrderLong {
        public final long id;
        public long quantity;
        public final long price;

        public OrderLong(long id, long qty, long price) {
            this.id = id;
            this.quantity = qty;
            this.price = price;
        }
    }

    /* ======================
     * Custom Ring-Buffer Queue
     * ====================== */
    public static final class OrderQueue {
        private final OrderLong[] items;
        private int head = 0;
        private int tail = 0;

        public OrderQueue(int capacity) {
            // REQUIRE: capacity is a power of two
            if (Integer.bitCount(capacity) != 1)
                throw new IllegalArgumentException("capacity must be power of two");
            this.items = new OrderLong[capacity];
        }

        public boolean add(OrderLong o) {
            int nextTail = (tail + 1) & (items.length - 1);
            if (nextTail == head) return false; // full
            items[tail] = o;
            tail = nextTail;
            return true;
        }

        public OrderLong peek() {
            if (head == tail) return null;
            return items[head];
        }

        public OrderLong poll() {
            if (head == tail) return null;
            OrderLong r = items[head];
            items[head] = null;
            head = (head + 1) & (items.length - 1);
            return r;
        }

        public boolean isEmpty() {
            return head == tail;
        }
    }

    /* ======================
     * Benchmark State
     * ====================== */
    @State(Scope.Thread)
    public static class QueueState {

        @Param({"128", "512", "1024"})
        int capacity;

        OrderQueue custom;
        LinkedList<OrderLong> linked;

        OrderLong[] testData;

        @Setup(Level.Iteration)
        public void setup() {
            int cap = capacity;

            // Custom queue (capacity must be power of two)
            int pow2 = Integer.highestOneBit(cap);
            if (pow2 < cap) pow2 *= 2;
            custom = new OrderQueue(pow2);

            linked = new LinkedList<>();

            // Preload objects once
            testData = new OrderLong[cap];
            for (int i = 0; i < cap; i++) {
                testData[i] = new OrderLong(i, 1, 100);
            }
        }
    }

    /* ======================
     * Benchmark: offer only
     * ====================== */
    @Benchmark
    public void custom_offer(QueueState s) {
        OrderQueue q = s.custom;
        for (OrderLong o : s.testData) {
            q.add(o);
        }
        // reset queue
        while (!q.isEmpty()) q.poll();
    }

    @Benchmark
    public void linked_offer(QueueState s) {
        LinkedList<OrderLong> q = s.linked;
        for (OrderLong o : s.testData) {
            q.add(o);
        }
        q.clear();
    }

    /* ======================
     * Benchmark: offer + poll
     * ====================== */
    @Benchmark
    public void custom_offer_poll(QueueState s, Blackhole bh) {
        OrderQueue q = s.custom;
        for (OrderLong o : s.testData) {
            q.add(o);
            bh.consume(q.poll());
        }
    }

    @Benchmark
    public void linked_offer_poll(QueueState s, Blackhole bh) {
        LinkedList<OrderLong> q = s.linked;
        for (OrderLong o : s.testData) {
            q.add(o);
            bh.consume(q.poll());
        }
    }

    /* ======================
     * Benchmark: FIFO rotate
     * simulates removal from middle
     * ====================== */
    @Benchmark
    public void custom_rotate(QueueState s) {
        OrderQueue q = s.custom;

        // prefill
        for (OrderLong o : s.testData) q.add(o);

        // rotate
        for (int i = 0; i < s.capacity; i++) {
            OrderLong o = q.poll();
            if (o == null) break;
            q.add(o);
        }

        // cleanup
        while (!q.isEmpty()) q.poll();
    }

    @Benchmark
    public void linked_rotate(QueueState s) {
        LinkedList<OrderLong> q = s.linked;

        // prefill
        for (OrderLong o : s.testData) q.add(o);

        // rotate
        for (int i = 0; i < s.capacity; i++) {
            OrderLong o = q.poll();
            if (o == null) break;
            q.add(o);
        }

        q.clear();
    }
}