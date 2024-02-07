package be.twofold.gbfr.gdex;

public final class GdexShort extends Gdex {
    private final short value;

    public GdexShort(GdexType type, short value) {
        super(type);
        this.value = value;
    }

    public short getValue() {
        return value;
    }

    @Override
    public short asShort() {
        return value;
    }
}
