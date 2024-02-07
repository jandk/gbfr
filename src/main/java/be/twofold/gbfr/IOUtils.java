package be.twofold.gbfr;

import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.function.*;

public final class IOUtils {
    private IOUtils() {
    }

    public static byte[] readBytes(ByteBuffer buffer, int length) {
        var bytes = new byte[length];
        buffer.get(bytes);
        return bytes;
    }

    public static String readString(ByteBuffer buffer) {
        var length = buffer.getInt();
        return readString(buffer, length);
    }

    public static String readString(ByteBuffer buffer, int length) {
        var bytes = readBytes(buffer, length);
        return new String(bytes);
    }

    public static long readInt48(ByteBuffer buffer) {
        var b0 = buffer.get();
        var b1 = buffer.get();
        var b2 = buffer.get();
        var b3 = buffer.get();
        var b4 = buffer.get();
        var b5 = buffer.get();

        return Byte.toUnsignedLong(b0)
            | (Byte.toUnsignedLong(b1) << 8)
            | (Byte.toUnsignedLong(b2) << 16)
            | (Byte.toUnsignedLong(b3) << 24)
            | (Byte.toUnsignedLong(b4) << 32)
            | (Byte.toUnsignedLong(b5) << 40);
    }

    public static String readWString(ByteBuffer buffer, int size) {
        var bytes = readBytes(buffer, size);
        return new String(bytes, 0, size, StandardCharsets.UTF_16LE);
    }

    public static int[] readIntArray(ByteBuffer buffer, int length) {
        var array = new int[length];
        for (var i = 0; i < length; i++) {
            array[i] = buffer.getInt();
        }
        return array;
    }

    public static UUID[] readGUIDArray(ByteBuffer buffer, int length) {
        var array = new UUID[length];
        for (var i = 0; i < length; i++) {
            var msb = Long.reverseBytes(buffer.getLong());
            var lsb = Long.reverseBytes(buffer.getLong());
            array[i] = new UUID(msb, lsb);
        }
        return array;
    }

    public static <T> List<T> readStructs(ByteBuffer buffer, int count, Function<ByteBuffer, T> reader) {
        var structs = new ArrayList<T>();
        for (var i = 0; i < count; i++) {
            structs.add(reader.apply(buffer));
        }
        return structs;
    }
}
