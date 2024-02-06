package be.twofold.gbfr.gts;

public final class GdexString extends Gdex {
    private final String value;

    public GdexString(GdexType type, String value) {
        super(type);
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String asString() {
        return value;
    }
}
