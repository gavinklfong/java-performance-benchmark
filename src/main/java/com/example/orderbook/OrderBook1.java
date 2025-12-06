package com.example.orderbook;

import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;


@Slf4j
public class OrderBook1 {
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

    // Print current book
    public void printBook() {
        log.info("\n--- ORDER BOOK ---");
        log.info("SELL:");
        sellBook.forEach((p, q) -> log.info(String.format("  %d -> %s%n", p, q)));
        log.info("BUY:");
        buyBook.forEach((p, q) -> log.info(String.format("  %d -> %s%n", p, q)));
    }

    public static void main(String[] args) {
        OrderBook1 ob = new OrderBook1();
        ob.insert(new OrderLong(1, OrderLong.Side.BUY, 100, 10));
        ob.insert(new OrderLong(2, OrderLong.Side.SELL, 101, 5));
        ob.insert(new OrderLong(3, OrderLong.Side.SELL, 99, 3));
        ob.insert(new OrderLong(4, OrderLong.Side.BUY, 102, 6));
        ob.cancel(1);
        ob.printBook();
    }
}
