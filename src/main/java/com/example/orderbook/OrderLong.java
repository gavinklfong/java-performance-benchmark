package com.example.orderbook;

class OrderLong {
    enum Side { BUY, SELL }
    final long id;
    final Side side;
    final long price;
    long quantity;

    OrderLong(long id, Side side, long price, long quantity) {
        this.id = id;
        this.side = side;
        this.price = price;
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "%s %d @ %d (id=%d)".formatted(side, quantity, price, id);
    }
}

