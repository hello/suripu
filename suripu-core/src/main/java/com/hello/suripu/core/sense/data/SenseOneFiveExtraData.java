package com.hello.suripu.core.sense.data;

import com.google.common.base.MoreObjects;

public class SenseOneFiveExtraData implements ExtraSensorData {

    private final int pressure;
    private final int tvoc;
    private final int co2;
    private final String rgb;
    private final int ir;
    private final int clear;
    private final int luxCount;
    private final int uvCount;

    private SenseOneFiveExtraData(int pressure, int tvoc, int co2, String rgb, int ir, int clear, int luxCount, int uvCount) {
        this.pressure = pressure;
        this.tvoc = tvoc;
        this.co2 = co2;
        this.rgb = rgb;
        this.ir = ir;
        this.clear = clear;
        this.luxCount = luxCount;
        this.uvCount = uvCount;
    }

    public static SenseOneFiveExtraData create(int pressure, int tvoc, int co2, String rgb, int ir, int clear, int luxCount, int uvCount) {
        return new SenseOneFiveExtraData(pressure, tvoc, co2, rgb, ir, clear, luxCount, uvCount);
    }

    @Override
    public int pressure() {
        return pressure;
    }

    @Override
    public int tvoc() {
        return tvoc;
    }

    @Override
    public int co2() {
        return co2;
    }

    @Override
    public String rgb() {
        return rgb;
    }

    @Override
    public int ir() {
        return ir;
    }

    @Override
    public int clear() {
        return clear;
    }

    @Override
    public int luxCount() {
        return luxCount;
    }

    @Override
    public int uvCount() {
        return uvCount;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(SenseOneFiveExtraData.class)
                .add("pressure", pressure())
                .add("tvoc", tvoc())
                .add("co2", co2())
                .add("rgb", rgb())
                .add("ir", ir())
                .add("clear", clear())
                .add("lux", luxCount())
                .add("uv", uvCount())
                .toString();
    }
}
