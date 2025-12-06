package com.example.orderbook;


import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(1)
public class OrderPoolBenchmark {

    /* ======================
       Order class
       ====================== */
    public static final class Order {
        public enum Side { BUY, SELL }

        public long id;
        public Side side;
        public long priceTicks;
        public long quantity;

        public Order() {}

        public Order(long id, Side side, long priceTicks, long quantity) {
            this.id = id;
            this.side = side;
            this.priceTicks = priceTicks;
            this.quantity = quantity;
        }

        public void reset(long id, Side side, long priceTicks, long quantity) {
            this.id = id;
            this.side = side;
            this.priceTicks = priceTicks;
            this.quantity = quantity;
        }
    }

    /* ======================
       Object Pool
       ====================== */
    public static final class OrderPool {
        private final Order[] pool;
        private int idx = 0;

        public OrderPool(int size) {
            pool = new Order[size];
            for (int i = 0; i < size; i++) pool[i] = new Order();
        }

        public Order acquire(long id, Order.Side side, long priceTicks, long quantity) {
            int i = idx++;
            if (i >= pool.length) idx = 0; // wrap around
            Order o = pool[i % pool.length];
            o.reset(id, side, priceTicks, quantity);
            return o;
        }

        public void release(Order o) {
            // nothing needed here; object is reused
        }
    }

    /* ======================
       Custom Ring Buffer Queue
       ====================== */
    public static final class OrderQueue {
        private final Order[] items;
        private int head = 0;
        private int tail = 0;

        public OrderQueue(int capacity) {
            if (Integer.bitCount(capacity) != 1)
                throw new IllegalArgumentException("Capacity must be power of 2");
            items = new Order[capacity];
        }

        public boolean add(Order o) {
            int nextTail = (tail + 1) & (items.length - 1);
            if (nextTail == head) return false; // full
            items[tail] = o;
            tail = nextTail;
            return true;
        }

        public Order poll() {
            if (head == tail) return null;
            Order o = items[head];
            items[head] = null;
            head = (head + 1) & (items.length - 1);
            return o;
        }

        public boolean isEmpty() {
            return head == tail;
        }
    }

    /* ======================
       Benchmark State
       ====================== */
    @State(Scope.Thread)
    public static class QueueState {
        @Param({"128", "512", "1024"})
        int capacity;

        OrderQueue customQueue1;
        OrderQueue customQueue2;
        OrderPool orderPool;

        long counter = 0;

        @Setup(Level.Iteration)
        public void setup() {
            // capacity must be power of 2 for OrderQueue
            int pow2 = Integer.highestOneBit(capacity);
            if (pow2 < capacity) pow2 <<= 1;
            customQueue1 = new OrderQueue(pow2);
            customQueue2 = new OrderQueue(pow2);
            orderPool = new OrderPool(capacity);
        }
    }

    /* ======================
       Benchmark: Normal queue
       ====================== */
    @Benchmark
    public void nonPooledQueueOfferPoll(QueueState s, Blackhole bh) {
        // simulate multiple cycles (fill queue, poll all)
        for (int i = 0; i < s.capacity; i++) {
            Order o = new Order(s.counter++, Order.Side.BUY, i, 1);
            s.customQueue1.add(o);
        }
        while (!s.customQueue1.isEmpty()) {
            bh.consume(s.customQueue1.poll());
        }
    }

    /* ======================
       Benchmark: Pooled queue
       ====================== */
    @Benchmark
    public void pooledQueueOfferPoll(QueueState s, Blackhole bh) {
        // fill queue using pooled objects
        for (int i = 0; i < s.capacity; i++) {
            Order o = s.orderPool.acquire(s.counter++, Order.Side.BUY, i, 1);
            s.customQueue2.add(o);
        }
        // poll all and release back to pool
        while (!s.customQueue2.isEmpty()) {
            Order o = s.customQueue2.poll();
            s.orderPool.release(o);
            bh.consume(o);
        }
    }
}

