package be.twofold.gbfr.encoder;

public record Png(
    PngFormat format,
    byte[] data
) {
}
