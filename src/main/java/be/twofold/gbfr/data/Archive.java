package be.twofold.gbfr.data;

import be.twofold.gbfr.data.*;
import net.jpountz.lz4.*;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;

public final class Archive implements AutoCloseable {
    private final SeekableByteChannel channel;

    public Archive(Path path) throws IOException {
        System.out.println("Opening archive: " + path);
        channel = Files.newByteChannel(path);
    }

    public ByteBuffer read(ChunkEntry entry) {
        try {
            var buffer = ByteBuffer
                .allocate(entry.size())
                .order(ByteOrder.LITTLE_ENDIAN);

            channel.position(entry.fileOffset());
            channel.read(buffer);

            if (entry.size() != entry.uncompressedSize()) {
                byte[] uncompressed = new byte[entry.uncompressedSize()];
                LZ4Factory
                    .safeInstance()
                    .safeDecompressor()
                    .decompress(buffer.array(), uncompressed);

                return ByteBuffer
                    .wrap(uncompressed)
                    .order(ByteOrder.LITTLE_ENDIAN);
            }

            return buffer.flip();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() throws Exception {
        channel.close();
    }
}
