package com.example.orderbook;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;


@Slf4j
public class OrderBook2 {

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


    private final List<Queue<OrderLong>> bids = new ArrayList<>(PRICE_LEVEL_COUNT);   // BUY side, index = priceTicks - MIN
    private final List<Queue<OrderLong>> asks = new ArrayList<>(PRICE_LEVEL_COUNT);   // SELL side

    private final TreeMap<Long, Queue<OrderLong>> buyBook =
            new TreeMap<>(Comparator.reverseOrder()); // highest price first
    private final TreeMap<Long, Queue<OrderLong>> sellBook =
            new TreeMap<>(); // lowest price first
    private final Map<Long, OrderLong> orderById = new HashMap<>();

    // Insert a new order
    public void insert(OrderLong order) {
        if (order.side == OrderLong.Side.BUY) {
            matchBuy(order);
            if (order.quantity > 0)
                addToBook(buyBook, order);
        } else {
            matchSell(order);
            if (order.quantity > 0)
                addToBook(sellBook, order);
        }
        if (order.quantity > 0)
            orderById.put(order.id, order);
    }

    // Cancel an order by ID
    public void cancel(long orderId) {
        OrderLong order = orderById.remove(orderId);
        if (order == null) return;

        TreeMap<Long, Queue<OrderLong>> book =
                (order.side == OrderLong.Side.BUY) ? buyBook : sellBook;
        Queue<OrderLong> queue = book.get(order.price);
        if (queue != null) {
            queue.remove(order);
            if (queue.isEmpty()) book.remove(order.price);
        }
        log.debug("Cancelled order " + orderId);
    }

    // Matching logic for BUY
    private void matchBuy(OrderLong buyOrder) {
        while (!sellBook.isEmpty() && buyOrder.quantity > 0) {
            Long bestSellPrice = sellBook.firstKey();
            if (bestSellPrice > buyOrder.price)
                break; // no match
            Queue<OrderLong> sells = sellBook.get(bestSellPrice);
            while (!sells.isEmpty() && buyOrder.quantity > 0) {
                OrderLong sellOrder = sells.peek();
                long traded = Math.min(buyOrder.quantity, sellOrder.quantity);
                buyOrder.quantity -= traded;
                sellOrder.quantity -= traded;
                log.debug(String.format("TRADE: %d @ %d (Buy %d vs Sell %d)%n",
                        traded, sellOrder.price, buyOrder.id, sellOrder.id));
                if (sellOrder.quantity == 0) {
                    sells.poll();
                    orderById.remove(sellOrder.id);
                }
            }
            if (sells.isEmpty()) sellBook.remove(bestSellPrice);
        }
    }

    // Matching logic for SELL
    private void matchSell(OrderLong sellOrder) {
        while (!buyBook.isEmpty() && sellOrder.quantity > 0) {
            long bestBuyPrice = buyBook.firstKey();
            if (bestBuyPrice < sellOrder.price)
                break; // no match
            Queue<OrderLong> buys = buyBook.get(bestBuyPrice);
            while (!buys.isEmpty() && sellOrder.quantity > 0) {
                OrderLong buyOrder = buys.peek();
                long traded = Math.min(sellOrder.quantity, buyOrder.quantity);
                sellOrder.quantity -= traded;
                buyOrder.quantity -= traded;
                log.debug(String.format("TRADE: %d @ %d (Sell %d vs Buy %d)%n",
                        traded, buyOrder.price, sellOrder.id, buyOrder.id));
                if (buyOrder.quantity == 0) {
                    buys.poll();
                    orderById.remove(buyOrder.id);
                }
            }
            if (buys.isEmpty()) buyBook.remove(bestBuyPrice);
        }
    }

    // Add remaining order to book
    private void addToBook(TreeMap<Long, Queue<OrderLong>> book, OrderLong order) {
        book.computeIfAbsent(order.price, p -> new LinkedList<>()).add(order);
    }

    private void matchOrAdd(OrderLong incoming) {
        if (incoming.quantity <= 0) return;
        if (incoming.side == OrderLong.Side.BUY) {
            // match against lowest asks
            int idx = priceToIndex(incoming.price);
            int askIdx = findBestAskIndexUpTo(idx);
            while (askIdx >= 0 && incoming.quantity > 0) {
                Queue<OrderLong> q = asks.get(askIdx);
                while (!q.isEmpty() && incoming.quantity > 0) {
                    OrderLong resting = q.peek();
                    long traded = Math.min(incoming.quantity, resting.quantity);
                    incoming.quantity -= traded;
                    resting.quantity -= traded;
                    onTrade(resting.price, traded, incoming.id, resting.id);
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
            int idx = priceToIndex(incoming.price);
            int bidIdx = findBestBidIndexDownTo(idx);
            while (bidIdx >= 0 && incoming.quantity > 0) {
                Queue<OrderLong> q = bids.get(bidIdx);
                while (!q.isEmpty() && incoming.quantity > 0) {
                    OrderLong resting = q.peek();
                    long traded = Math.min(incoming.quantity, resting.quantity);
                    incoming.quantity -= traded;
                    resting.quantity -= traded;
                    onTrade(resting.price, traded, resting.id, incoming.id);
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

    /* ---------- Helpers: price index mapping and best-level searches ---------- */
    private static int priceToIndex(long priceTicks) {
        int idx = (int) (priceTicks - MIN_PRICE_TICKS);
        if (idx < 0) idx = 0; if (idx >= PRICE_LEVEL_COUNT) idx = PRICE_LEVEL_COUNT - 1;
        return idx;
    }

    // Find lowest ask index that has orders and price <= limitIdx (start at low end)
    private int findBestAskIndexUpTo(int maxIdx) {
        // scan from lowest ask price upwards until maxIdx
        for (int i = 0; i <= maxIdx; i++) if (!asks.get(i).isEmpty()) return i;
        // if none in range, find any ask (best available) above maxIdx
        for (int i = maxIdx + 1; i < PRICE_LEVEL_COUNT; i++) if (!asks.get(i).isEmpty()) return i;
        return -1;
    }

    // find next ask index starting from candidate
    private int findNextAskIndex(int start) {
        for (int i = start; i < PRICE_LEVEL_COUNT; i++) if (!asks.get(i).isEmpty()) return i;
        return -1;
    }

    // Find highest bid index that has orders and price >= limitIdx
    private int findBestBidIndexDownTo(int minIdx) {
        for (int i = PRICE_LEVEL_COUNT - 1; i >= minIdx; i--) if (!bids.get(i).isEmpty()) return i;
        for (int i = minIdx - 1; i >= 0; i--) if (!bids.get(i).isEmpty()) return i;
        return -1;
    }

    private int findNextBidIndex(int start) {
        for (int i = start; i >= 0; i--) if (!bids.get(i).isEmpty()) return i;
        return -1;
    }

    // Add buy order to bids
    private void addBuy(OrderLong o) {
        int idx = priceToIndex(o.price);
        boolean ok = bids.get(idx).add(o);
        if (!ok) {
            // level full - simple fallback: reject or expand in production
            System.err.println("BUY level full; rejecting order=" + o.id);
        } else {
            orderById.put(o.id, o);
        }
    }

    // Add sell order to asks
    private void addSell(OrderLong o) {
        int idx = priceToIndex(o.price);
        boolean ok = asks.get(idx).add(o);
        if (!ok) {
            System.err.println("SELL level full; rejecting order=" + o.id);
        } else {
            orderById.put(o.id, o);
        }
    }

    protected void onTrade(long priceTicks, long quantity, long buyOrderId, long sellOrderId) {
        // In production you'd publish trade, persist, and update P&L etc.
        log.debug(String.format("TRADE %d @ %.2f (buy=%d sell=%d)", quantity, priceTicks / (double) TICK_MULTIPLIER, buyOrderId, sellOrderId));
    }

    // Print current book
    public void printBook() {
        log.info("\n--- ORDER BOOK ---");
        log.info("SELL:");
        sellBook.forEach((p, q) -> log.info(String.format("  %d -> %s%n", p, q)));
        log.info("BUY:");
        buyBook.forEach((p, q) -> log.info(String.format("  %d -> %s%n", p, q)));
    }

    public static void main(String[] args) {
        OrderBook2 ob = new OrderBook2();
        ob.insert(new OrderLong(1, OrderLong.Side.BUY, 100, 10));
        ob.insert(new OrderLong(2, OrderLong.Side.SELL, 101, 5));
        ob.insert(new OrderLong(3, OrderLong.Side.SELL, 99, 3));
        ob.insert(new OrderLong(4, OrderLong.Side.BUY, 102, 6));
        ob.cancel(1);
        ob.printBook();
    }
}
