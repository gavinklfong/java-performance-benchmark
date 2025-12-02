package com.example.orderbook;

import java.math.BigDecimal;

class Order {
    enum Side { BUY, SELL }
    final long id;
    final Side side;
    final BigDecimal price;
    long quantity;

    Order(long id, Side side, BigDecimal price, long quantity) {
        this.id = id;
        this.side = side;
        this.price = price;
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "%s %d @ %.2f (id=%d)".formatted(side, quantity, price, id);
    }
}

