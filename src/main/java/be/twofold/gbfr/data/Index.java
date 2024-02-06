package be.twofold.gbfr.data;

import be.twofold.gbfr.*;
import net.jpountz.xxhash.*;

import java.io.*;
import java.nio.*;
import java.nio.file.*;

public record Index(
    String codename,
    short numArchives,
    long[] archiveFilesHashTable,
    ChunkIndex[] fileToChunkIndicesTable,
    ChunkEntry[] chunkEntryTable,
    long[] externalFilesHashTable,
    long[] externalFilesSizesTable,
    int[] cachedChunkIndicesTable
) {
    private static final XXHash64 XxHash64 = XXHashFactory.safeInstance().hash64();

    public static Index read(Path path) throws IOException {
        var buffer = ByteBuffer
            .wrap(Files.readAllBytes(path))
            .order(ByteOrder.LITTLE_ENDIAN);

        var fieldTableOffset = buffer.getInt();
        buffer.position(buffer.position() + 6);
        var fieldOffsetTableSize = buffer.getShort();
        var offsetsToFields = new short[(fieldOffsetTableSize - 2) / 2];
        for (int i = 0; i < offsetsToFields.length; i++) {
            offsetsToFields[i] = buffer.getShort();
        }

        var codename = readCodename(buffer, fieldTableOffset + offsetsToFields[1]);
        var numArchives = readNumArchives(buffer, fieldTableOffset + offsetsToFields[2]);
        // 3 is invalid
        var archiveFilesHashTable = readArchiveFilesHashTable(buffer, fieldTableOffset + offsetsToFields[4]);
        var fileToChunkIndicesTable = readFileToChunkIndicesTable(buffer, fieldTableOffset + offsetsToFields[5]);
        var chunkEntryTable = readChunkEntryTable(buffer, fieldTableOffset + offsetsToFields[6]);
        var externalFilesHashTable = readExternalFilesHashTable(buffer, fieldTableOffset + offsetsToFields[7]);
        var externalFilesSizesTable = readExternalFilesSizeTable(buffer, fieldTableOffset + offsetsToFields[8]);
        var cachedChunkIndicesTable = readCachedChunkIndicesTable(buffer, fieldTableOffset + offsetsToFields[9]);

        return new Index(
            codename,
            numArchives,
            archiveFilesHashTable,
            fileToChunkIndicesTable,
            chunkEntryTable,
            externalFilesHashTable,
            externalFilesSizesTable,
            cachedChunkIndicesTable
        );
    }

    private static String readCodename(ByteBuffer buffer, int offset) {
        buffer.position(offset);
        var strOffset = buffer.getInt();

        buffer.position(buffer.position() + strOffset - 4);
        return IOUtils.readString(buffer);
    }

    private static short readNumArchives(ByteBuffer buffer, int offset) {
        buffer.position(offset);
        return buffer.getShort();
    }

    private static long[] readArchiveFilesHashTable(ByteBuffer buffer, int offset) {
        buffer.position(offset);
        var tableOffset = buffer.getInt();

        buffer.position(buffer.position() + tableOffset - 4);
        var entriesCount = buffer.getInt();

        long[] archiveFilesHashTable = new long[entriesCount];
        for (int i = 0; i < entriesCount; i++) {
            archiveFilesHashTable[i] = buffer.getLong();
        }
        return archiveFilesHashTable;
    }

    private static ChunkIndex[] readFileToChunkIndicesTable(ByteBuffer buffer, int offset) {
        buffer.position(offset);
        var tableOffset = buffer.getInt();

        buffer.position(buffer.position() + tableOffset - 4);
        var entriesCount = buffer.getInt();

        ChunkIndex[] fileToChunkIndicesTable = new ChunkIndex[entriesCount];
        for (int i = 0; i < entriesCount; i++) {
            fileToChunkIndicesTable[i] = ChunkIndex.read(buffer);
        }
        return fileToChunkIndicesTable;
    }

    private static ChunkEntry[] readChunkEntryTable(ByteBuffer buffer, int offset) {
        buffer.position(offset);
        var tableOffset = buffer.getInt();

        buffer.position(buffer.position() + tableOffset - 4);
        var entriesCount = buffer.getInt();

        ChunkEntry[] chunkEntryTable = new ChunkEntry[entriesCount];
        for (int i = 0; i < entriesCount; i++) {
            chunkEntryTable[i] = ChunkEntry.read(buffer);
        }
        return chunkEntryTable;
    }

    private static long[] readExternalFilesHashTable(ByteBuffer buffer, int offset) {
        buffer.position(offset);
        var tableOffset = buffer.getInt();

        buffer.position(buffer.position() + tableOffset - 4);
        var entriesCount = buffer.getInt();

        long[] externalFilesHashTable = new long[entriesCount];
        for (int i = 0; i < entriesCount; i++) {
            externalFilesHashTable[i] = buffer.getLong();
        }
        return externalFilesHashTable;
    }

    private static long[] readExternalFilesSizeTable(ByteBuffer buffer, int offset) {
        buffer.position(offset);
        var tableOffset = buffer.getInt();

        buffer.position(buffer.position() + tableOffset - 4);
        var entriesCount = buffer.getInt();

        long[] externalFilesSizeTable = new long[entriesCount];
        for (int i = 0; i < entriesCount; i++) {
            externalFilesSizeTable[i] = buffer.getLong();
        }
        return externalFilesSizeTable;
    }

    private static int[] readCachedChunkIndicesTable(ByteBuffer buffer, int offset) {
        buffer.position(offset);
        var tableOffset = buffer.getInt();

        buffer.position(buffer.position() + tableOffset - 4);
        var entriesCount = buffer.getInt();

        int[] cachedChunkIndicesTable = new int[entriesCount];
        for (int i = 0; i < entriesCount; i++) {
            cachedChunkIndicesTable[i] = buffer.getInt();
        }
        return cachedChunkIndicesTable;
    }

    public ChunkEntry getEntry(String file) {
        var hash = XxHash64.hash(ByteBuffer.wrap(file.getBytes()), 0);

        var entry = ArrayUtils.indexOf(archiveFilesHashTable, hash);
        if (entry < 0) {
            throw new IllegalArgumentException("File not found: " + file);
        }

        var chunkIndex = fileToChunkIndicesTable[entry];
        return chunkEntryTable[chunkIndex.chunkEntryIndex()];
    }
}
