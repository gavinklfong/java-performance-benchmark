package com.example.orderbook;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(2)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class OrderBook2Benchmark {

    @Param({"50000"})     // how many orders to preload
    int preloadOrders;

    @Param({"100000"})    // price tick range (must match your OrderBook2)
    int priceTickRange;

    OrderBook2 book;
    List<OrderLong> preloaded;

    long orderIdCounter;

    @Setup(Level.Iteration)
    public void setup() {
        book = new OrderBook2();
        preloaded = new ArrayList<>(preloadOrders);

        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        for (int i = 0; i < preloadOrders; i++) {
            long id = ++orderIdCounter;
            long priceTicks = rnd.nextInt(priceTickRange);
            long qty = rnd.nextInt(1, 50);

            OrderLong.Side side = (rnd.nextBoolean()
                    ? OrderLong.Side.BUY
                    : OrderLong.Side.SELL);

            OrderLong o = new OrderLong(id, side, priceTicks, qty);

            book.insert(o);
            preloaded.add(o);
        }
    }

    /** Benchmark inserting a new random order */
//    @Benchmark
//    public void benchInsert(Blackhole bh) {
//        ThreadLocalRandom rnd = ThreadLocalRandom.current();
//        long id = ++orderIdCounter;
//
//        long priceTicks = rnd.nextInt(priceTickRange);
//        long qty = rnd.nextInt(1, 40);
//        OrderLong.Side side = rnd.nextBoolean() ? OrderLong.Side.BUY : OrderLong.Side.SELL;
//
//        OrderLong incoming = new OrderLong(id, side, priceTicks, qty);
//        book.insert(incoming);
//
//        bh.consume(incoming);
//    }

    /** Benchmark cancelling a random existing order */
    @Benchmark
    public void benchCancel(Blackhole bh) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        OrderLong picked = preloaded.get(rnd.nextInt(preloaded.size()));

        book.cancel(picked.id);

        bh.consume(picked);
    }

    /** Benchmark matching behaviour by sending a new aggressive order */
    @Benchmark
    public void benchMatch(Blackhole bh) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        OrderLong template = preloaded.get(rnd.nextInt(preloaded.size()));

        long id = ++orderIdCounter;
        long priceTicks = template.price;
        long qty = rnd.nextInt(5, 80);

        // opposite side to force matching
        OrderLong.Side opposite = (template.side == OrderLong.Side.BUY)
                ? OrderLong.Side.SELL
                : OrderLong.Side.BUY;

        OrderLong incoming = new OrderLong(id, opposite, priceTicks, qty);

        book.insert(incoming);

        bh.consume(incoming);
    }
}
