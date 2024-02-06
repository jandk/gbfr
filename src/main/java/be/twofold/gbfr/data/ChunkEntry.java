package be.twofold.gbfr.data;

import java.nio.*;

public record ChunkEntry(
    long fileOffset,
    int uncompressedSize,
    int size,
    int allocAlignment,
    boolean unkBool,
    byte dataFileNumber
) {
    public static ChunkEntry read(ByteBuffer buffer) {
        var fileOffset = buffer.getLong();
        var size = buffer.getInt();
        var uncompressedSize = buffer.getInt();
        var allocAlignment = buffer.getInt();
        var unkBool = buffer.get() != 0;
        buffer.get();
        var dataFileNumber = buffer.get();
        buffer.get();

        return new ChunkEntry(
            fileOffset,
            uncompressedSize,
            size,
            allocAlignment,
            unkBool,
            dataFileNumber
        );
    }
}
