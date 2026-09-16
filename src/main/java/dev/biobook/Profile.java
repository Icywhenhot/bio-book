package dev.biobook;

public record Profile(String name, String subtitle, String lore) {
    public static final Profile EMPTY = new Profile("", "", "");
    public static final int NAME_LIMIT = 48;
    public static final int SUBTITLE_LIMIT = 96;
    public static final int TEXT_LIMIT = 4096;

    public Profile {
        name = clean(name, NAME_LIMIT, false);
        subtitle = clean(subtitle, SUBTITLE_LIMIT, false);
        lore = clean(lore, TEXT_LIMIT, true);
    }

    public static String clean(String value, int limit, boolean multiline) {
        StringBuilder result = new StringBuilder();
        value.codePoints().filter(c -> c != 167 && !Character.isISOControl(c) || multiline && c == '\n')
                .forEach(c -> {
                    if (result.length() + Character.charCount(c) <= limit) result.appendCodePoint(c);
                });
        return result.toString();
    }
}
