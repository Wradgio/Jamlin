package sk.cw.jamlin;

public enum JamlinAction {
    EXTRACT,
    REPLACE;

    public static JamlinAction fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return EXTRACT;
        }
        for (JamlinAction action : values()) {
            if (action.name().equalsIgnoreCase(value.trim())) {
                return action;
            }
        }
        return null;
    }

    public String getCliName() {
        return name().toLowerCase();
    }
}
