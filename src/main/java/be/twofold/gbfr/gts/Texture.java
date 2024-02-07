package be.twofold.gbfr.gts;

import java.util.*;

public record Texture(
    String name,
    int width,
    int height,
    int offsetX,
    int offsetY,
    String address,
    int[] srgb,
    UUID[] thumbnail
) {
}
