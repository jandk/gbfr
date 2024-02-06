package be.twofold.gbfr.data;

import java.nio.*;

public record ChunkIndex(
    int chunkEntryIndex,
    int fileSize,
    int offsetIntoDecompressedChunk
) {
    public static ChunkIndex read(ByteBuffer buffer) {
        var chunkEntryIndex = buffer.getInt();
        var fileSize = buffer.getInt();
        var offsetIntoDecompressedChunk = buffer.getInt();

        return new ChunkIndex(
            chunkEntryIndex,
            fileSize,
            offsetIntoDecompressedChunk
        );
    }
}
