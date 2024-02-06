package be.twofold.gbfr;

import java.nio.*;
import java.nio.charset.*;
import java.util.*;

public final class IOUtils {
    private IOUtils() {
    }

    public static byte[] readBytes(ByteBuffer buffer, int length) {
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return bytes;
    }

    public static String readString(ByteBuffer buffer) {
        int length = buffer.getInt();
        return readString(buffer, length);
    }

    public static String readString(ByteBuffer buffer, int length) {
        byte[] bytes = readBytes(buffer, length);
        return new String(bytes);
    }

    public static long readInt48(ByteBuffer buffer) {
        byte b0 = buffer.get();
        byte b1 = buffer.get();
        byte b2 = buffer.get();
        byte b3 = buffer.get();
        byte b4 = buffer.get();
        byte b5 = buffer.get();

        return Byte.toUnsignedLong(b0)
            | (Byte.toUnsignedLong(b1) << 8)
            | (Byte.toUnsignedLong(b2) << 16)
            | (Byte.toUnsignedLong(b3) << 24)
            | (Byte.toUnsignedLong(b4) << 32)
            | (Byte.toUnsignedLong(b5) << 40);
    }

    public static String readWString(ByteBuffer buffer, int size) {
        byte[] bytes = readBytes(buffer, size);
        return new String(bytes, 0, size, StandardCharsets.UTF_16LE);
    }

    public static int[] readIntArray(ByteBuffer buffer, int length) {
        int[] array = new int[length];
        for (int i = 0; i < length; i++) {
            array[i] = buffer.getInt();
        }
        return array;
    }

    public static UUID[] readGUIDArray(ByteBuffer buffer, int length) {
        UUID[] array = new UUID[length];
        for (int i = 0; i < length; i++) {
            long msb = Long.reverseBytes(buffer.getLong());
            long lsb = Long.reverseBytes(buffer.getLong());
            array[i] = new UUID(msb, lsb);
        }
        return array;
    }
}
