package com.yarnandthread.app.model;

public class Counter {

    public String name;
    public int value;
    public int max;       // 0 = no max
    public int linkedTo;  // -1 = no link (index of target counter)

    public Counter() {
        this.linkedTo = -1;
    }

    public Counter(String name) {
        this.name = name;
        this.value = 0;
        this.max = 0;
        this.linkedTo = -1;
    }

    public boolean hasMax() {
        return max > 0;
    }

    public boolean hasLink() {
        return linkedTo >= 0;
    }
}
