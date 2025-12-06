package com.example.orderbook;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;


@Slf4j
public class OrderBook {
    private final TreeMap<BigDecimal, Queue<OrderBigDecimal>> buyBook =
            new TreeMap<>(Comparator.reverseOrder()); // highest price first
    private final TreeMap<BigDecimal, Queue<OrderBigDecimal>> sellBook =
            new TreeMap<>(); // lowest price first
    private final Map<Long, OrderBigDecimal> orderById = new HashMap<>();

    // Insert a new order
    public void insert(OrderBigDecimal order) {
        if (order.side == OrderBigDecimal.Side.BUY) {
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
        OrderBigDecimal order = orderById.remove(orderId);
        if (order == null) return;

        TreeMap<BigDecimal, Queue<OrderBigDecimal>> book =
                (order.side == OrderBigDecimal.Side.BUY) ? buyBook : sellBook;
        Queue<OrderBigDecimal> queue = book.get(order.price);
        if (queue != null) {
            queue.remove(order);
            if (queue.isEmpty()) book.remove(order.price);
        }
        log.debug("Cancelled order " + orderId);
    }

    // Matching logic for BUY
    private void matchBuy(OrderBigDecimal buyOrder) {
        while (!sellBook.isEmpty() && buyOrder.quantity > 0) {
            BigDecimal bestSellPrice = sellBook.firstKey();
            if (bestSellPrice.compareTo(buyOrder.price) > 0)
                break; // no match
            Queue<OrderBigDecimal> sells = sellBook.get(bestSellPrice);
            while (!sells.isEmpty() && buyOrder.quantity > 0) {
                OrderBigDecimal sellOrder = sells.peek();
                long traded = Math.min(buyOrder.quantity, sellOrder.quantity);
                buyOrder.quantity -= traded;
                sellOrder.quantity -= traded;
                log.debug(String.format("TRADE: %d @ %.2f (Buy %d vs Sell %d)%n",
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
    private void matchSell(OrderBigDecimal sellOrder) {
        while (!buyBook.isEmpty() && sellOrder.quantity > 0) {
            BigDecimal bestBuyPrice = buyBook.firstKey();
            if (bestBuyPrice.compareTo(sellOrder.price) < 0)
                break; // no match
            Queue<OrderBigDecimal> buys = buyBook.get(bestBuyPrice);
            while (!buys.isEmpty() && sellOrder.quantity > 0) {
                OrderBigDecimal buyOrder = buys.peek();
                long traded = Math.min(sellOrder.quantity, buyOrder.quantity);
                sellOrder.quantity -= traded;
                buyOrder.quantity -= traded;
                log.debug(String.format("TRADE: %d @ %.2f (Sell %d vs Buy %d)%n",
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
    private void addToBook(TreeMap<BigDecimal, Queue<OrderBigDecimal>> book, OrderBigDecimal order) {
        book.computeIfAbsent(order.price, p -> new LinkedList<>()).add(order);
    }

    // Print current book
    public void printBook() {
        log.info("\n--- ORDER BOOK ---");
        log.info("SELL:");
        sellBook.forEach((p, q) -> log.info(String.format("  %.2f -> %s%n", p, q)));
        log.info("BUY:");
        buyBook.forEach((p, q) -> log.info(String.format("  %.2f -> %s%n", p, q)));
    }

    public static void main(String[] args) {
        OrderBook ob = new OrderBook();
        ob.insert(new OrderBigDecimal(1, OrderBigDecimal.Side.BUY, BigDecimal.valueOf(100.0), 10));
        ob.insert(new OrderBigDecimal(2, OrderBigDecimal.Side.SELL, BigDecimal.valueOf(101.0), 5));
        ob.insert(new OrderBigDecimal(3, OrderBigDecimal.Side.SELL, BigDecimal.valueOf(99.0), 3));
        ob.insert(new OrderBigDecimal(4, OrderBigDecimal.Side.BUY, BigDecimal.valueOf(102.0), 6));
        ob.cancel(1);
        ob.printBook();
    }
}
