package com.example.orderbook;

import java.math.BigDecimal;
import java.util.*;

public class OrderBook {
    private final TreeMap<BigDecimal, Queue<Order>> buyBook =
            new TreeMap<>(Comparator.reverseOrder()); // highest price first
    private final TreeMap<BigDecimal, Queue<Order>> sellBook =
            new TreeMap<>(); // lowest price first
    private final Map<Long, Order> orderById = new HashMap<>();

    // Insert a new order
    public void insert(Order order) {
        if (order.side == Order.Side.BUY) {
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
        Order order = orderById.remove(orderId);
        if (order == null) return;

        TreeMap<BigDecimal, Queue<Order>> book =
                (order.side == Order.Side.BUY) ? buyBook : sellBook;
        Queue<Order> queue = book.get(order.price);
        if (queue != null) {
            queue.remove(order);
            if (queue.isEmpty()) book.remove(order.price);
        }
        System.out.println("Cancelled order " + orderId);
    }

    // Matching logic for BUY
    private void matchBuy(Order buyOrder) {
        while (!sellBook.isEmpty() && buyOrder.quantity > 0) {
            BigDecimal bestSellPrice = sellBook.firstKey();
            if (bestSellPrice.compareTo(buyOrder.price) > 0)
                break; // no match
            Queue<Order> sells = sellBook.get(bestSellPrice);
            while (!sells.isEmpty() && buyOrder.quantity > 0) {
                Order sellOrder = sells.peek();
                long traded = Math.min(buyOrder.quantity, sellOrder.quantity);
                buyOrder.quantity -= traded;
                sellOrder.quantity -= traded;
                System.out.printf("TRADE: %d @ %.2f (Buy %d vs Sell %d)%n",
                        traded, sellOrder.price, buyOrder.id, sellOrder.id);
                if (sellOrder.quantity == 0) {
                    sells.poll();
                    orderById.remove(sellOrder.id);
                }
            }
            if (sells.isEmpty()) sellBook.remove(bestSellPrice);
        }
    }

    // Matching logic for SELL
    private void matchSell(Order sellOrder) {
        while (!buyBook.isEmpty() && sellOrder.quantity > 0) {
            BigDecimal bestBuyPrice = buyBook.firstKey();
            if (bestBuyPrice.compareTo(sellOrder.price) < 0)
                break; // no match
            Queue<Order> buys = buyBook.get(bestBuyPrice);
            while (!buys.isEmpty() && sellOrder.quantity > 0) {
                Order buyOrder = buys.peek();
                long traded = Math.min(sellOrder.quantity, buyOrder.quantity);
                sellOrder.quantity -= traded;
                buyOrder.quantity -= traded;
                System.out.printf("TRADE: %d @ %.2f (Sell %d vs Buy %d)%n",
                        traded, buyOrder.price, sellOrder.id, buyOrder.id);
                if (buyOrder.quantity == 0) {
                    buys.poll();
                    orderById.remove(buyOrder.id);
                }
            }
            if (buys.isEmpty()) buyBook.remove(bestBuyPrice);
        }
    }

    // Add remaining order to book
    private void addToBook(TreeMap<BigDecimal, Queue<Order>> book, Order order) {
        book.computeIfAbsent(order.price, p -> new LinkedList<>()).add(order);
    }

    // Print current book
    public void printBook() {
        System.out.println("\n--- ORDER BOOK ---");
        System.out.println("SELL:");
        sellBook.forEach((p, q) -> System.out.printf("  %.2f -> %s%n", p, q));
        System.out.println("BUY:");
        buyBook.forEach((p, q) -> System.out.printf("  %.2f -> %s%n", p, q));
    }

    public static void main(String[] args) {
        OrderBook ob = new OrderBook();
        ob.insert(new Order(1, Order.Side.BUY, BigDecimal.valueOf(100.0), 10));
        ob.insert(new Order(2, Order.Side.SELL, BigDecimal.valueOf(101.0), 5));
        ob.insert(new Order(3, Order.Side.SELL, BigDecimal.valueOf(99.0), 3));
        ob.insert(new Order(4, Order.Side.BUY, BigDecimal.valueOf(102.0), 6));
        ob.cancel(1);
        ob.printBook();
    }
}
