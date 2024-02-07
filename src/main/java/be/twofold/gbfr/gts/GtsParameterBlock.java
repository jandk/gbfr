package be.twofold.gbfr.gts;

import java.nio.*;

public record GtsParameterBlock(
    int id,
    int codec,
    int size,
    int offset,
    int pad
) {
    public static GtsParameterBlock read(ByteBuffer buffer) {
        var id = buffer.getInt();
        var codec = buffer.getInt();
        var size = buffer.getInt();
        var offset = buffer.getInt();
        var pad = buffer.getInt();

        return new GtsParameterBlock(id, codec, size, offset, pad);
    }
}
