package com.example.orderbook;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(value = 2)
public class TreeMapOrderBookBenchmark {

    private OrderBook book;
    private List<OrderBigDecimal> orders; // pre-generated orders

    @Param({"100000"}) // large number of price levels
    int priceLevels;

    @Setup(Level.Iteration)
    public void setup() {
        book = new OrderBook();
        orders = new ArrayList<>(priceLevels);

        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        // preload book with many price levels
        for (int i = 0; i < priceLevels; i++) {
            BigDecimal price = BigDecimal.valueOf(50_000 + rnd.nextInt(100_000))
                    .setScale(2, BigDecimal.ROUND_HALF_UP);

            OrderBigDecimal.Side side =
                    rnd.nextBoolean() ? OrderBigDecimal.Side.BUY : OrderBigDecimal.Side.SELL;

            OrderBigDecimal order = new OrderBigDecimal(i, side, price, 10);
            book.insert(order);
            orders.add(order);
        }
    }

//    @Benchmark
//    public void benchmarkInsert(Blackhole bh) {
//        ThreadLocalRandom rnd = ThreadLocalRandom.current();
//
//        BigDecimal price = BigDecimal.valueOf(50_000 + rnd.nextInt(100_000))
//                .setScale(2, BigDecimal.ROUND_HALF_UP);
//
//        OrderBigDecimal order =
//                new OrderBigDecimal(rnd.nextLong(), OrderBigDecimal.Side.BUY, price, 5);
//
//        book.insert(order);
//
//        bh.consume(order);
//    }

    @Benchmark
    public void benchmarkMatch(Blackhole bh) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        OrderBigDecimal template = orders.get(rnd.nextInt(orders.size()));

        OrderBigDecimal incoming =
                new OrderBigDecimal(rnd.nextLong(), template.side, template.price, 20);

        book.insert(incoming); // triggers matchBuy or matchSell

        bh.consume(incoming);
    }

    @Benchmark
    public void benchmarkCancel(Blackhole bh) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        OrderBigDecimal o = orders.get(rnd.nextInt(orders.size()));

        book.cancel(o.id);

        bh.consume(o);
    }
}

