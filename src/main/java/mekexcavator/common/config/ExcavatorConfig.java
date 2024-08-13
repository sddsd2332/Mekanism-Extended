package mekexcavator.common.config;

public class ExcavatorConfig {

    private static ExcavatorConfig LOCAL = new ExcavatorConfig();
    private static ExcavatorConfig  SERVER = null;

    public static ExcavatorConfig current() {
        return SERVER != null ? SERVER : LOCAL;
    }
    public static ExcavatorConfig local() {
        return LOCAL;
    }

}
