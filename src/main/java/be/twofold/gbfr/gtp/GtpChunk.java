package be.twofold.gbfr.gtp;

import be.twofold.gbfr.util.*;

import java.nio.*;

public record GtpChunk(
    int compression,
    int parameterId,
    int size,
    byte[] data
) {
    public static GtpChunk read(ByteBuffer buffer) {
        var compression = buffer.getInt();
        var parameterId = buffer.getInt();
        var size = buffer.getInt();
        var data = IOUtils.readBytes(buffer, size);

        return new GtpChunk(compression, parameterId, size, data);
    }
}
