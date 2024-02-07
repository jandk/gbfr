package be.twofold.gbfr.gtp;

import be.twofold.gbfr.util.*;

import java.nio.*;
import java.util.*;

public record Gtp(
    GtpHeader header,
    List<List<GtpChunk>> chunks
) {
    public static Gtp read(ByteBuffer buffer) {
        var header = GtpHeader.read(buffer);
        if(header.magic() != 0x50415247) {
            return null;
        }

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
