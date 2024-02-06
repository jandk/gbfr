package be.twofold.gbfr;

import be.twofold.gbfr.data.*;

import java.io.*;
import java.nio.file.*;

public class Main {
    public static void main(String[] args) throws IOException {

        Path root = Path.of(args[0]);

        Index index = Index.read(root.resolve("data.i"));

    }
}
