package be.twofold.gbfr.gts;

import be.twofold.gbfr.*;

import java.nio.*;
import java.util.*;

record Gtp(
    GtpHeader header,
    List<List<GtpChunk>> chunks
) {
    public static Gtp read(ByteBuffer buffer) {
        var header = GtpHeader.read(buffer);

        int numBlocks = (buffer.limit() + 0x7ffff) / 0x80000;

        List<List<GtpChunk>> chunks = new ArrayList<>();
        for (int i = 0; i < numBlocks; i++) {
            int start = i * 0x80000;
            buffer.position(i == 0 ? 24 : start);

            var chunkOffsets = IOUtils.readIntArray(buffer, buffer.getInt());

            List<GtpChunk> subChunks = new ArrayList<>();
            for (int chunkOffset : chunkOffsets) {
                buffer.position(start + chunkOffset);
                subChunks.add(GtpChunk.read(buffer));
            }
            chunks.add(subChunks);
        }

        return new Gtp(header, chunks);
    }
}
