package mg.miniframework.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DataTypeUtils {

    private static String dateFormat = "yyyy-MM-dd";

    public static String getDateFormat() {
        return dateFormat;
    }

    public static void setDateFormat(String dateFormat) {
        DataTypeUtils.dateFormat = dateFormat;
    }

    public static Object convertParam(String value, Class<?> type) {
        if (value == null)
            return null;

        if (type == String.class)
            return value;
        if (type == int.class || type == Integer.class)
            return Integer.parseInt(value);
        if (type == long.class || type == Long.class)
            return Long.parseLong(value);
        if (type == double.class || type == Double.class)
            return Double.parseDouble(value);
        if (type == float.class || type == Float.class)
            return Float.parseFloat(value);
        if (type == boolean.class || type == Boolean.class)
            return Boolean.parseBoolean(value);

        DateTimeFormatter userFormat = null;
        try {
            userFormat = DateTimeFormatter.ofPattern(dateFormat);
        } catch (Exception ignored) {
        }

        DateTimeFormatter[] dateFormats = new DateTimeFormatter[] {
                userFormat,
                DateTimeFormatter.ISO_DATE,
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd")
        };

        DateTimeFormatter[] dateTimeFormats = new DateTimeFormatter[] {
                userFormat,
                DateTimeFormatter.ISO_DATE_TIME,
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        };

        if (type == LocalDate.class) {
            for (DateTimeFormatter f : dateFormats) {
                if (f == null)
                    continue;
                try {
                    return LocalDate.parse(value, f);
                } catch (Exception ignored) {
                }
            }
            throw new IllegalArgumentException("Format LocalDate non supporté : " + value);
        }

        if (type == LocalDateTime.class) {
            for (DateTimeFormatter f : dateTimeFormats) {
                if (f == null)
                    continue;
                try {
                    return LocalDateTime.parse(value, f);
                } catch (Exception ignored) {
                }
            }
            throw new IllegalArgumentException("Format LocalDateTime non supporté : " + value);
        }

        if (type == Date.class) {

            for (DateTimeFormatter f : dateTimeFormats) {
                if (f == null)
                    continue;
                try {
                    LocalDateTime dt = LocalDateTime.parse(value, f);
                    return Date.from(dt.atZone(ZoneId.systemDefault()).toInstant());
                } catch (Exception ignored) {
                }
            }

            for (DateTimeFormatter f : dateFormats) {
                if (f == null)
                    continue;
                try {
                    LocalDate d = LocalDate.parse(value, f);
                    return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
                } catch (Exception ignored) {
                }
            }

            throw new IllegalArgumentException("Format Date non supporté : " + value);
        }

        return value;
    }

}
