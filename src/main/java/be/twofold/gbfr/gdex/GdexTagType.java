package be.twofold.gbfr.gdex;

enum GdexTagType {
    Object(1),
    String(2),
    Short(3),
    IntArray(8),
    GUIDArray(13);

    private final int value;

    GdexTagType(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static GdexTagType fromValue(int value) {
        for (var type : values()) {
            if (type.value() == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown GdexType value: " + value);
    }
}
