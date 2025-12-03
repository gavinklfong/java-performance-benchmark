package com.example.orderbook;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class OrderBook1Benchmark {

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

        long randomPrice() {
            // 99.00 – 101.00
            return 99 + rand.nextInt() * 2L;
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

        OrderBook1 orderBook;
        Random rand;

        @Setup(Level.Iteration)
        public void setup() {
            orderBook = new OrderBook1();
            rand = new Random(999);

            preloadBuyOrders();
            preloadSellOrders();
        }

        private void preloadBuyOrders() {
            for (int i = 0; i < buyLevels; i++) {
                long price = 100 - i;

                for (int j = 0; j < ordersPerLevel; j++) {
                    orderBook.insert(new OrderLong(
                            nextId(),
                            OrderLong.Side.BUY,
                            price,
                            randQty()
                    ));
                }
            }
        }

        private void preloadSellOrders() {
            for (int i = 0; i < sellLevels; i++) {
                long price = 100 + i;

                for (int j = 0; j < ordersPerLevel; j++) {
                    orderBook.insert(new OrderLong(
                            nextId(),
                            OrderLong.Side.SELL,
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
        OrderLong order = new OrderLong(
                ts.randomId(),
                OrderLong.Side.BUY,
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
        OrderLong order = new OrderLong(
                ts.randomId(),
                OrderLong.Side.SELL,
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