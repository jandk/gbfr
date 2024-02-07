package be.twofold.gbfr.exporter;

public record XmlTexture(
    String src,
    int subIndex,
    int width,
    int height,
    int numChannels
) {
}
