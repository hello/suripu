package com.hello.suripu.core.firmware;

public enum Product {
    SENSE(1),
    PILL(2);

    private int value;

    Product(int value) {
        this.value = value;
    }

    public static Product fromInt(int value) {
        for(Product version : Product.values()) {
            if (version.value == value) {
                return version;
            }
        }

        throw new IllegalArgumentException(String.format("Unknown product: %d", value));
    }
}