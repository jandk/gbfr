package be.twofold.gbfr.exporter;

import java.util.*;

public record XmlAsset(
    String name,
    UUID guid,
    int width,
    int height,
    List<XmlTexture> textures
) {
}
