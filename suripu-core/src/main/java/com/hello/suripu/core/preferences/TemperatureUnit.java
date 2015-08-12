package com.hello.suripu.core.preferences;

public enum TemperatureUnit {
    CELSIUS("C"),
    FAHRENHEIT("F");

    private String value;

    TemperatureUnit(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
