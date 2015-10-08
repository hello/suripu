package com.hello.suripu.core.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.hello.suripu.core.models.Device;
import com.hello.suripu.core.models.device.v2.Pill;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by pangwu on 1/8/15.
 */
public class PillColorUtil {

    public static Color BLUE = new Color(0x00, 0x00, 0xFE);
    public static Color RED = new Color(0xFE, 0x00, 0x00);
    public static Color AQUA = new Color(0x00, 0xFE, 0xFE);
    public static Color YELLOW = new Color(0xFE, 0xFE, 0x00);

    public static List<Color> getPillColors(){
        final java.util.List<Color> pillColors = new ArrayList<>();
        pillColors.add(BLUE);
        pillColors.add(RED);
        pillColors.add(AQUA);
        pillColors.add(YELLOW);

        return pillColors;
    }


    public static int argbToIntBasedOnSystemEndianess(final byte[] argb){
        if(ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN)){
            return ByteBuffer.wrap(argb).order(ByteOrder.BIG_ENDIAN).getInt();
        }

        return ByteBuffer.wrap(argb).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public static byte[] colorToARGB(final Color color){
        final byte[] argb = new byte[4];
        argb[0] = (byte)color.getAlpha();
        argb[1] = (byte)color.getRed();
        argb[2] = (byte)color.getGreen();
        argb[3] = (byte)color.getBlue();
        return argb;
    }

    private static Map<Integer,Device.Color> colorMap;
    static {
        Map<Integer, Device.Color> aMap = Maps.newHashMap();
        aMap.put(argbToIntBasedOnSystemEndianess(colorToARGB(BLUE)), Device.Color.BLUE);
        aMap.put(argbToIntBasedOnSystemEndianess(colorToARGB(RED)), Device.Color.RED);
        aMap.put(argbToIntBasedOnSystemEndianess(colorToARGB(AQUA)), Device.Color.AQUA);
        aMap.put(argbToIntBasedOnSystemEndianess(colorToARGB(YELLOW)), Device.Color.YELLOW);
        colorMap= ImmutableMap.copyOf(aMap);
    }

    public static Device.Color displayDeviceColor(int color) {
        if(colorMap.containsKey(color)) {
            return colorMap.get(color);
        }

        return Device.Color.BLUE;
    }


    private static Map<Integer,Color> awtColorMap;
    static {
        Map<Integer, Color> aMap = Maps.newHashMap();
        aMap.put(argbToIntBasedOnSystemEndianess(colorToARGB(BLUE)), BLUE);
        aMap.put(argbToIntBasedOnSystemEndianess(colorToARGB(RED)), RED);
        aMap.put(argbToIntBasedOnSystemEndianess(colorToARGB(AQUA)), AQUA);
        aMap.put(argbToIntBasedOnSystemEndianess(colorToARGB(YELLOW)), YELLOW);
        awtColorMap= ImmutableMap.copyOf(aMap);
    }

    public static Color pillColor(int color) {
        if(awtColorMap.containsKey(color)) {
            return awtColorMap.get(color);
        }

        return BLUE;
    }

    public static Pill.Color displayPillColor(int color) {
        return Pill.Color.fromDeviceColor(displayDeviceColor(color));
    }
}
