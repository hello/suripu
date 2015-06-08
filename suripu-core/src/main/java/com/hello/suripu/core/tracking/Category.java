package com.hello.suripu.core.tracking;

public enum Category {
    UPTIME(1),
    GHOST_WAVE(2);

    public final int value;

    Category(int value) {
        this.value = value;
    }


    public static Category fromString(final String name) {
        for(final Category category : Category.values()) {
            if(category.toString().toLowerCase().equals(name.toLowerCase())) {
                return category;
            }
        }

        throw new IllegalArgumentException(String.format("%s is not a valid category", name));
    }
}
