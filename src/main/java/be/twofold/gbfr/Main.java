package be.twofold.gbfr;

import be.twofold.gbfr.data.*;
import net.jpountz.xxhash.*;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

public class Main {
    private static final List<String> Files = List.of(
        "granite/2k/gts/0/0.gts",
        "granite/2k/gts/1/1.gts",
        "granite/2k/gts/2/2.gts",
        "granite/4k/gts/0/0.gts",
        "granite/4k/gts/1/1.gts",
        "granite/4k/gts/2/2.gts"
    );

    private final Index index;
    private final List<Archive> archives;

    public Main(Index index, List<Archive> archives) {
        this.index = index;
        this.archives = archives;
    }

    public static void main(String[] args) throws IOException {
        var main = load(Path.of(args[0]));

        for (var file : Files) {
            var buffer = main.readFile(file);
        }
    }

    private static Main load(Path root) throws IOException {
        var index = Index.read(root.resolve("data.i"));
        var archives = new ArrayList<Archive>();
        for (var i = 0; i < index.numArchives(); i++) {
            archives.add(new Archive(root.resolve("data." + i)));
        }

        return new Main(index, archives);
    }

    private ByteBuffer readFile(String file) {
        var chunkEntry = index.getEntry(file);
        var archive = archives.get(chunkEntry.dataFileNumber());
        return archive.read(chunkEntry);
    }

}
