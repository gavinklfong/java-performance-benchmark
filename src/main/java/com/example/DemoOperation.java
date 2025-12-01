package com.example;

import java.util.random.RandomGenerator;

public class DemoOperation {

    private final RandomGenerator randomGenerator;
    private final int loopCount;

    public DemoOperation(int loopCount) {
        this.loopCount = loopCount;
        this.randomGenerator = RandomGenerator.getDefault();
    }

    public long calculateRandomSum() {
        long sum = 0;
        for (int i = 0; i < loopCount; i++) {
            sum += randomGenerator.nextInt(100, 1000);
        }

        return sum;
    }

    public static void main(String[] args) {
        DemoOperation demo = new DemoOperation(100);
        long startTime = System.nanoTime();
        demo.calculateRandomSum();
        long duration = System.nanoTime() - startTime;
        System.out.println("execution time: " + duration + " ns");
    }
}
