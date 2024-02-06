package be.twofold.gbfr.gts;

import be.twofold.gbfr.*;

import java.nio.*;

record GtpHeader(
    int magic,
    int version,
    byte[] md5Sum,
    int[] chunkOffsets
) {
    public static GtpHeader read(ByteBuffer buffer) {
        var magic = buffer.getInt();
        var version = buffer.getInt();
        var md5Sum = IOUtils.readBytes(buffer, 16);
        var chunkCount = buffer.getInt();
        var chunkOffsets = new int[chunkCount];
        for (int i = 0; i < chunkCount; i++) {
            chunkOffsets[i] = buffer.getInt();
        }

        return new GtpHeader(magic, version, md5Sum, chunkOffsets);
    }
}
