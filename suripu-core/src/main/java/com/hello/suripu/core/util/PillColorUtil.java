package com.hello.suripu.core.util;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

/**
 * Created by pangwu on 1/8/15.
 */
public class PillColorUtil {
    public static Color getPillColorByAccountRegistrationOrder(final int orderIndex){
        final ArrayList<Color> pillColors = new ArrayList<>();
        pillColors.add(new Color(0x00, 0x00, 0xFE));
        pillColors.add(new Color(0xFE, 0x00, 0x00));
        pillColors.add(new Color(0x00, 0xFE, 0xFE));
        pillColors.add(new Color(0xFE, 0xFE, 0x00));

        return pillColors.get(orderIndex % pillColors.size());
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
}
