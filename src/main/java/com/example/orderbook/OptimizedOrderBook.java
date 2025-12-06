package com.example.orderbook;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;


public final class OptimizedOrderBook {

    /* ---------- Configuration: tune these for your market ---------- */
    // Price ticks: 1 tick = 1/100 of currency unit (e.g., cents)
    public static final long TICK_MULTIPLIER = 100L; // two decimal places

    // Define supported price range (in ticks). Keep this range small for memory efficiency.
    // Example: support prices from 0.00 to 1000.00 -> 100000 ticks
    public static final long MIN_PRICE_TICKS = 0L;
    public static final long MAX_PRICE_TICKS = 1000_00L; // 1000.00 -> 100000 ticks
    private static final int PRICE_LEVEL_COUNT = (int) (MAX_PRICE_TICKS - MIN_PRICE_TICKS + 1);

    // Capacity per price level (max number of resting orders per level)
    private static final int PER_LEVEL_CAPACITY = 256; // power of two preferred

    // Event batch size consumed by matching thread
    private static final int BATCH_SIZE = 64;

    /* ---------- Domain objects ---------- */
    public static final class Order {
        public enum Side { BUY, SELL }

        // Mutable order stored off of heap or pooled
        public long id;
        public Side side;
        public long priceTicks; // integer price
        public long quantity;   // quantity remaining

        public void reset(long id, Side side, long priceTicks, long quantity) {
            this.id = id; this.side = side; this.priceTicks = priceTicks; this.quantity = quantity;
        }

        @Override
        public String toString() {
            return String.format("%s %d @ %.2f (id=%d)", side, quantity, priceTicks / (double) TICK_MULTIPLIER, id);
        }
    }

    /**
     * Very small object pool for Orders. Not production grade (no GC-free reclaim),
     * but adequate for demo and to reduce allocations.
     */
    public static final class OrderPool {
        private final Order[] pool;
        private final AtomicInteger idx = new AtomicInteger(0);

        public OrderPool(int size) {
            pool = new Order[size];
            for (int i = 0; i < pool.length; i++) pool[i] = new Order();
        }

        public Order acquire(long id, Order.Side side, long priceTicks, long quantity) {
            int i = Math.floorMod(idx.getAndIncrement(), pool.length);
            Order o = pool[i];
            o.reset(id, side, priceTicks, quantity);
            return o;
        }
    }

    /**
     * Simple ring buffer for orders at a single price level. Fixed-size array to avoid allocations.
     */
    public static final class OrderQueue {
        private final Order[] items;
        private int head = 0; // points at next element to poll
        private int tail = 0; // points at next element to insert

        public OrderQueue(int capacity) {
            // capacity must be >= 1
            items = new Order[capacity];
        }

        public boolean add(Order o) {
            int nextTail = (tail + 1) & (items.length - 1);
            if (nextTail == head) return false; // full
            items[tail] = o;
            tail = nextTail;
            return true;
        }

        public Order peek() {
            if (head == tail) return null;
            return items[head];
        }

        public Order poll() {
            if (head == tail) return null;
            Order r = items[head];
            items[head] = null; // help debugging; pool owns objects
            head = (head + 1) & (items.length - 1);
            return r;
        }

        public boolean isEmpty() { return head == tail; }
    }

    /* ---------- Order book data structures (single-threaded access assumed) ---------- */
    // Price-level arrays for bids and asks (O(1) by price index)
    private final OrderQueue[] bids = new OrderQueue[PRICE_LEVEL_COUNT];   // BUY side, index = priceTicks - MIN
    private final OrderQueue[] asks = new OrderQueue[PRICE_LEVEL_COUNT];   // SELL side

    // Track order by id for cancelation (simple HashMap). In production you might use a primitive map.
    private final Map<Long, Order> orderById = new HashMap<>(1 << 16);

    // simple concurrent producer queue: producers enqueue events here
    private final ConcurrentLinkedQueue<OrderEvent> producerQueue = new ConcurrentLinkedQueue<>();

    private final OrderPool pool = new OrderPool(1024 * 16);

    public OptimizedOrderBook() {
        // initialize per-level ring buffers. use power-of-two capacity for fast masking.
        int cap = 1;
        while (cap < PER_LEVEL_CAPACITY) cap <<= 1;
        for (int i = 0; i < PRICE_LEVEL_COUNT; i++) {
            bids[i] = new OrderQueue(cap);
            asks[i] = new OrderQueue(cap);
        }
    }

    /* ---------- Events submitted by producers ---------- */
    public static final class OrderEvent {
        enum Type { INSERT, CANCEL }
        public Type type;
        public long orderId;
        public Order.Side side; // only for INSERT
        public long priceTicks; // only for INSERT
        public long quantity;   // only for INSERT

        public void setInsert(long id, Order.Side side, long priceTicks, long qty) {
            this.type = Type.INSERT; this.orderId = id; this.side = side; this.priceTicks = priceTicks; this.quantity = qty;
        }

        public void setCancel(long id) {
            this.type = Type.CANCEL; this.orderId = id;
        }
    }

    /* ---------- Producer API (thread-safe) ---------- */
    public void submitInsert(long orderId, Order.Side side, long priceTicks, long quantity) {
        OrderEvent e = new OrderEvent();
        e.setInsert(orderId, side, priceTicks, quantity);
        producerQueue.offer(e);
    }

    public void submitCancel(long orderId) {
        OrderEvent e = new OrderEvent();
        e.setCancel(orderId);
        producerQueue.offer(e);
    }

    /* ---------- Matching loop (single-threaded) ---------- */
    public void runMatchingLoop() {
        final List<OrderEvent> batch = new ArrayList<>(BATCH_SIZE);
        while (true) {
            // gather a small batch
            batch.clear();
            OrderEvent ev = producerQueue.poll();
            if (ev == null) {
                // busy-spin or sleep briefly in real engine. Keep simple here.
                Thread.yield();
                continue;
            }
            batch.add(ev);
            for (int i = 1; i < BATCH_SIZE; i++) {
                ev = producerQueue.poll();
                if (ev == null) break;
                batch.add(ev);
            }

            // process batch
            for (OrderEvent e : batch) {
                if (e.type == OrderEvent.Type.INSERT) {
                    // allocate from pool
                    Order o = pool.acquire(e.orderId, e.side, e.priceTicks, e.quantity);
                    // perform matching in single-thread
                    matchOrAdd(o);
                } else {
                    cancelInternal(e.orderId);
                }
            }

            // In a real engine you'd also flush trade events, persist, publish market data, etc.
        }
    }

    /* ---------- Matching logic (single-threaded) ---------- */
    private void matchOrAdd(Order incoming) {
        if (incoming.quantity <= 0) return;
        if (incoming.side == Order.Side.BUY) {
            // match against lowest asks
            int idx = priceToIndex(incoming.priceTicks);
            int askIdx = findBestAskIndexUpTo(idx);
            while (askIdx >= 0 && incoming.quantity > 0) {
                OrderQueue q = asks[askIdx];
                while (!q.isEmpty() && incoming.quantity > 0) {
                    Order resting = q.peek();
                    long traded = Math.min(incoming.quantity, resting.quantity);
                    incoming.quantity -= traded;
                    resting.quantity -= traded;
                    onTrade(resting.priceTicks, traded, incoming.id, resting.id);
                    if (resting.quantity == 0) {
                        q.poll();
                        orderById.remove(resting.id);
                    }
                }
                if (q.isEmpty()) {
                    // move to next best ask (higher index)
                    askIdx = findNextAskIndex(askIdx + 1);
                } else {
                    break; // incoming depleted or best ask still has quantity
                }
            }
            if (incoming.quantity > 0) addBuy(incoming);
        } else {
            // SELL: match against highest bids
            int idx = priceToIndex(incoming.priceTicks);
            int bidIdx = findBestBidIndexDownTo(idx);
            while (bidIdx >= 0 && incoming.quantity > 0) {
                OrderQueue q = bids[bidIdx];
                while (!q.isEmpty() && incoming.quantity > 0) {
                    Order resting = q.peek();
                    long traded = Math.min(incoming.quantity, resting.quantity);
                    incoming.quantity -= traded;
                    resting.quantity -= traded;
                    onTrade(resting.priceTicks, traded, resting.id, incoming.id);
                    if (resting.quantity == 0) {
                        q.poll();
                        orderById.remove(resting.id);
                    }
                }
                if (q.isEmpty()) {
                    bidIdx = findNextBidIndex(bidIdx - 1);
                } else {
                    break;
                }
            }
            if (incoming.quantity > 0) addSell(incoming);
        }
    }

    // Add buy order to bids
    private void addBuy(Order o) {
        int idx = priceToIndex(o.priceTicks);
        boolean ok = bids[idx].add(o);
        if (!ok) {
            // level full - simple fallback: reject or expand in production
            System.err.println("BUY level full; rejecting order=" + o.id);
        } else {
            orderById.put(o.id, o);
        }
    }

    // Add sell order to asks
    private void addSell(Order o) {
        int idx = priceToIndex(o.priceTicks);
        boolean ok = asks[idx].add(o);
        if (!ok) {
            System.err.println("SELL level full; rejecting order=" + o.id);
        } else {
            orderById.put(o.id, o);
        }
    }

    private void cancelInternal(long orderId) {
        Order o = orderById.remove(orderId);
        if (o == null) return;
        int idx = priceToIndex(o.priceTicks);
        OrderQueue q = (o.side == Order.Side.BUY) ? bids[idx] : asks[idx];
        // remove by scanning the small ring buffer; in production you'd have a doubly-linked list or index
        // this scan is limited to PER_LEVEL_CAPACITY so it's bounded and cheap in typical cases
        removeFromQueue(q, o);
        System.out.println("CANCELLED " + orderId);
    }

    private void removeFromQueue(OrderQueue q, Order target) {
        // naive remove: rotate until found
        int cap = q.items.length;
        for (int i = 0; i < cap; i++) {
            Order o = q.peek();
            if (o == null) break;
            if (o == target) { q.poll(); return; }
            // move head to tail: poll and add back
            Order moved = q.poll();
            if (moved == null) break;
            q.add(moved);
        }
    }

    /* ---------- Helpers: price index mapping and best-level searches ---------- */
    private static int priceToIndex(long priceTicks) {
        int idx = (int) (priceTicks - MIN_PRICE_TICKS);
        if (idx < 0) idx = 0; if (idx >= PRICE_LEVEL_COUNT) idx = PRICE_LEVEL_COUNT - 1;
        return idx;
    }

    // Find lowest ask index that has orders and price <= limitIdx (start at low end)
    private int findBestAskIndexUpTo(int maxIdx) {
        // scan from lowest ask price upwards until maxIdx
        for (int i = 0; i <= maxIdx; i++) if (!asks[i].isEmpty()) return i;
        // if none in range, find any ask (best available) above maxIdx
        for (int i = maxIdx + 1; i < PRICE_LEVEL_COUNT; i++) if (!asks[i].isEmpty()) return i;
        return -1;
    }

    // find next ask index starting from candidate
    private int findNextAskIndex(int start) {
        for (int i = start; i < PRICE_LEVEL_COUNT; i++) if (!asks[i].isEmpty()) return i;
        return -1;
    }

    // Find highest bid index that has orders and price >= limitIdx
    private int findBestBidIndexDownTo(int minIdx) {
        for (int i = PRICE_LEVEL_COUNT - 1; i >= minIdx; i--) if (!bids[i].isEmpty()) return i;
        for (int i = minIdx - 1; i >= 0; i--) if (!bids[i].isEmpty()) return i;
        return -1;
    }

    private int findNextBidIndex(int start) {
        for (int i = start; i >= 0; i--) if (!bids[i].isEmpty()) return i;
        return -1;
    }

    /* ---------- Trade callback (deliver trade) ---------- */
    protected void onTrade(long priceTicks, long quantity, long buyOrderId, long sellOrderId) {
        // In production you'd publish trade, persist, and update P&L etc.
        System.out.printf("TRADE %d @ %.2f (buy=%d sell=%d)\n", quantity, priceTicks / (double) TICK_MULTIPLIER, buyOrderId, sellOrderId);
    }

    /* ---------- Demo main ---------- */
    public static void main(String[] args) throws InterruptedException {
        OptimizedOrderBook engine = new OptimizedOrderBook();

        // start matcher thread
        Thread matcher = new Thread(engine::runMatchingLoop, "matcher");
        matcher.setDaemon(true);
        matcher.start();

        // simulate producers
        // We'll submit a few inserts and cancels
        engine.submitInsert(1L, Order.Side.BUY, 10000L, 10); // 100.00
        engine.submitInsert(2L, Order.Side.SELL,  9900L, 5);  // 99.00
        engine.submitInsert(3L, Order.Side.SELL, 10100L, 5);  // 101.00
        engine.submitInsert(4L, Order.Side.BUY, 10200L, 7);  // 102.00
        Thread.sleep(100);
        engine.submitCancel(3L);

        // let the engine run
        Thread.sleep(1000);
    }
}

