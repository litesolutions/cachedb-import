package es.litesolutions.cache.util;

import java.util.Map;
import java.util.Objects;

public final class Argument
{
    private Argument()
    {
        throw new Error("instantiation not permitted");
    }

    public static String read(final String key, final Map<String, String> map)
    {
        Objects.requireNonNull(key);
        Objects.requireNonNull(map);
        final String value = map.get(key);
        if (value == null)
            throw new IllegalArgumentException("required argument " + key
                + " is missing");
        return value;
    }

    public static String readOrDefault(final String key,
        final Map<String, String> map, final String defaultValue)
    {
        Objects.requireNonNull(key);
        Objects.requireNonNull(map);
        Objects.requireNonNull(defaultValue);
        return map.getOrDefault(key, defaultValue);
    }
}
