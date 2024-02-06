package be.twofold.gbfr.gts;

import java.util.*;

public record Texture(
    String name,
    short width,
    short height,
    short offsetX,
    short offsetY,
    String address,
    int[] srgb,
    UUID[] thumbnail
) {
}
