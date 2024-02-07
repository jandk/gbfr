package be.twofold.gbfr.gts;

import be.twofold.gbfr.util.*;

import java.nio.*;

public record GtsPageFile(
    String name,
    int numPages,
    byte[] checksum,
    int type,
    int sizeInBytes,
    int padding
) {
    public static GtsPageFile read(ByteBuffer buffer) {
        var name = IOUtils.readWString(buffer, 0x200).trim();
        var numPages = buffer.getInt();
        var checksum = IOUtils.readBytes(buffer, 0x10);
        var type = buffer.getInt();
        var sizeInBytes = buffer.getInt();
        var padding = buffer.getInt();

        return new GtsPageFile(
            name,
            numPages,
            checksum,
            type,
            sizeInBytes,
            padding
        );
    }
}
