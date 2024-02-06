package be.twofold.gbfr.gts;

import java.nio.*;
import java.util.*;

record GtpFile(
    GtpHeader header,
    List<GtpChunk> chunks
) {
    public static GtpFile read(ByteBuffer buffer) {
        var header = GtpHeader.read(buffer);
        var chunks = new ArrayList<GtpChunk>();
        for (int i = 0; i < header.chunkOffsets().length; i++) {
            buffer.position(header.chunkOffsets()[i]);
            chunks.add(GtpChunk.read(buffer));
        }
        return new GtpFile(header, chunks);
    }
}
