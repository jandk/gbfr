package be.twofold.gbfr;

import java.nio.*;

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
        byte[] bytes = readBytes(buffer, length);
        return new String(bytes);
    }
}
