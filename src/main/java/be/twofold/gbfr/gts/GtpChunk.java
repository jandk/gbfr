package be.twofold.gbfr.gts;

import be.twofold.gbfr.*;

import java.nio.*;

record GtpChunk(
    int versionMaybe,
    int magicMaybe,
    int size,
    byte[] data
) {
    public static GtpChunk read(ByteBuffer buffer) {
        var versionMaybe = buffer.getInt();
        var magicMaybe = buffer.getInt();
        var size = buffer.getInt();
        var data = IOUtils.readBytes(buffer, size);

        return new GtpChunk(versionMaybe, magicMaybe, size, data);
    }
}
