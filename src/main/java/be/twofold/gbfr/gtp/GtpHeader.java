package be.twofold.gbfr.gtp;

import be.twofold.gbfr.util.*;

import java.nio.*;

record GtpHeader(
    int magic,
    int version,
    byte[] md5Sum
) {
    public static GtpHeader read(ByteBuffer buffer) {
        var magic = buffer.getInt();
        var version = buffer.getInt();
        var md5Sum = IOUtils.readBytes(buffer, 16);

        return new GtpHeader(magic, version, md5Sum);
    }
}
