package com.example.orderbook;

import org.openjdk.jmh.annotations.*;
import java.math.BigDecimal;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class OrderBookBenchmark {

    @State(Scope.Thread)
    public static class ThreadState {
        Random rand;

        @Setup(Level.Trial)
        public void setup() {
            rand = new Random(123);
        }

        long randomId() {
            return rand.nextInt(1_000_000);
        }

        BigDecimal randomPrice() {
            // 99.00 – 101.00
            return BigDecimal.valueOf(99 + rand.nextDouble() * 2)
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
        }

        long randomQuantity() {
            return rand.nextInt(10) + 1;
        }
    }

    @State(Scope.Benchmark)
    public static class SharedState {

        @Param({"500"})  // number of BUY price levels
        int buyLevels;

        @Param({"500"})  // number of SELL price levels
        int sellLevels;

        @Param({"1000"}) // orders per price level
        int ordersPerLevel;

        OrderBook orderBook;
        Random rand;

        @Setup(Level.Iteration)
        public void setup() {
            orderBook = new OrderBook();
            rand = new Random(999);

            preloadBuyOrders();
            preloadSellOrders();
        }

        private void preloadBuyOrders() {
            for (int i = 0; i < buyLevels; i++) {
                BigDecimal price = BigDecimal.valueOf(100 - i * 0.01)
                        .setScale(2, BigDecimal.ROUND_HALF_UP);

                for (int j = 0; j < ordersPerLevel; j++) {
                    orderBook.insert(new OrderBigDecimal(
                            nextId(),
                            OrderBigDecimal.Side.BUY,
                            price,
                            randQty()
                    ));
                }
            }
        }

        private void preloadSellOrders() {
            for (int i = 0; i < sellLevels; i++) {
                BigDecimal price = BigDecimal.valueOf(100 + i * 0.01)
                        .setScale(2, BigDecimal.ROUND_HALF_UP);

                for (int j = 0; j < ordersPerLevel; j++) {
                    orderBook.insert(new OrderBigDecimal(
                            nextId(),
                            OrderBigDecimal.Side.SELL,
                            price,
                            randQty()
                    ));
                }
            }
        }

        long nextId() {
            return rand.nextInt(10_000_000);
        }

        long randQty() {
            return rand.nextInt(20) + 1;
        }
    }

    // ------------------------------
    // Benchmark: Insert BUY orders
    // ------------------------------
    @Benchmark
    public void insertBuy(SharedState state, ThreadState ts) {
        OrderBigDecimal order = new OrderBigDecimal(
                ts.randomId(),
                OrderBigDecimal.Side.BUY,
                ts.randomPrice(),
                ts.randomQuantity()
        );
        state.orderBook.insert(order);
    }

    // ------------------------------
    // Benchmark: Insert SELL orders
    // ------------------------------
    @Benchmark
    public void insertSell(SharedState state, ThreadState ts) {
        OrderBigDecimal order = new OrderBigDecimal(
                ts.randomId(),
                OrderBigDecimal.Side.SELL,
                ts.randomPrice(),
                ts.randomQuantity()
        );
        state.orderBook.insert(order);
    }

    // ------------------------------
    // Benchmark: Cancel orders
    // ------------------------------
    @Benchmark
    public void cancelRandom(SharedState state, ThreadState ts) {
        long id = ts.randomId();
        state.orderBook.cancel(id);
    }
}