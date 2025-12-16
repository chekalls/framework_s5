package mg.miniframework.utils;

import java.lang.reflect.*;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataTypeUtils {

    private static String dateFormat = "yyyy-MM-dd";

    public static Map<?, ?> resolveMapForParameter(Type paramType,
            Map<Path, byte[]> fileMap,
            Map<String, Object> mapParameters) {

        if (!(paramType instanceof ParameterizedType pt)) {
            return new HashMap<>();
        }

        Type[] typeArgs = pt.getActualTypeArguments();
        if (typeArgs.length != 2) {
            return new HashMap<>();
        }

        Class<?> keyType = (Class<?>) typeArgs[0];
        Class<?> valueType = (Class<?>) typeArgs[1];

        if (keyType == Path.class && valueType == byte[].class && fileMap != null && !fileMap.isEmpty()) {
            boolean allMatch = fileMap.entrySet().stream()
                    .allMatch(e -> e.getKey() instanceof Path && e.getValue() instanceof byte[]);
            if (allMatch) {
                return fileMap;
            }
        }

        if (keyType == String.class && valueType == Object.class && mapParameters != null && !mapParameters.isEmpty()) {
            boolean allMatch = mapParameters.keySet().stream().allMatch(k -> k instanceof String);
            if (allMatch) {
                return mapParameters;
            }
        }

        return new HashMap<>();
    }

    public static boolean isMapOfType(Object obj, Class<?> keyType, Class<?> valueType) {
        if (!(obj instanceof Map<?, ?> map)) {
            return false;
        }

        if (map.isEmpty()) {
            return false;
        }

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!keyType.equals(entry.getKey().getClass())) {
                return false;
            }

            if (!valueType.equals(entry.getValue().getClass())) {
                return false;
            }
        }

        return true;
    }

    public static boolean isPrimitiveOrWrapper(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            return true;
        }

        if (clazz == Boolean.class || clazz == Byte.class || clazz == Character.class
                || clazz == Short.class || clazz == Integer.class || clazz == Long.class
                || clazz == Float.class || clazz == Double.class || clazz == Void.class) {
            return true;
        }

        if (clazz == String.class) {
            return true;
        }

        return false;
    }

    public static boolean isListType(Class<?> clazz) {
        return List.class.isAssignableFrom(clazz);
    }

    @SuppressWarnings("unchecked")
    public static Object convertListToTargetType(List<Object> valueList, Class<?> targetType, Class<?> elementType)
            throws Exception {
        if (targetType.isArray()) {
            Object array = Array.newInstance(targetType.getComponentType(), valueList.size());
            for (int i = 0; i < valueList.size(); i++) {
                Object val = valueList.get(i);
                Array.set(array, i, convertElement(val, targetType.getComponentType()));
            }
            return array;
        }

        if (Collection.class.isAssignableFrom(targetType)) {
            Collection<Object> collection;
            if (!targetType.isInterface()) {
                try {
                    collection = (Collection<Object>) targetType.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    collection = new ArrayList<>();
                }
            } else {
                collection = new ArrayList<>();
            }

            for (Object val : valueList) {
                collection.add(convertElement(val, elementType != null ? elementType : Object.class));
            }

            return collection;
        }

        throw new IllegalArgumentException("Type cible non supporté : " + targetType.getName());
    }

    private static Object convertElement(Object value, Class<?> targetType) {
        if (value == null)
            return null;
        if (targetType.isInstance(value))
            return value;
        if (targetType == String.class)
            return value.toString();
        if (targetType == Integer.class || targetType == int.class)
            return Integer.parseInt(value.toString());
        if (targetType == Long.class || targetType == long.class)
            return Long.parseLong(value.toString());
        if (targetType == Double.class || targetType == double.class)
            return Double.parseDouble(value.toString());
        if (targetType == Float.class || targetType == float.class)
            return Float.parseFloat(value.toString());
        if (targetType == Boolean.class || targetType == boolean.class)
            return Boolean.parseBoolean(value.toString());
        return value;
    }

    public static boolean isArrayType(Class<?> clazz) {
        if (clazz.isArray())
            return true;

        if (Collection.class.isAssignableFrom(clazz))
            return true;

        if (Map.class.isAssignableFrom(clazz))
            return true;

        return false;
    }

    public static Class<?> getContentType(Class<?> clazz) {
        if (clazz.isArray()) {
            // Tableau natif
            return clazz.getComponentType();
        } else if (Collection.class.isAssignableFrom(clazz)) {
            // Collection générique
            // On ne peut pas récupérer le type exact à partir de Class<?>
            // Ici, on retourne Object.class par défaut
            return Object.class;
        } else if (Map.class.isAssignableFrom(clazz)) {
            // Map : retourner le type des valeurs
            return Object.class;
        } else {
            // Type simple
            return clazz;
        }
    }

    /**
     * Variante pour un Field, qui peut contenir le type générique si disponible
     */
    public static Class<?> getContentType(Field field) {
        Class<?> clazz = field.getType();

        if (clazz.isArray()) {
            return clazz.getComponentType();
        } else if (Collection.class.isAssignableFrom(clazz)) {
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType) {
                Type[] typeArgs = ((ParameterizedType) genericType).getActualTypeArguments();
                if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                    return (Class<?>) typeArgs[0];
                }
            }
            return Object.class; // par défaut si le generic n'est pas disponible
        } else if (Map.class.isAssignableFrom(clazz)) {
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType) {
                Type[] typeArgs = ((ParameterizedType) genericType).getActualTypeArguments();
                if (typeArgs.length > 1 && typeArgs[1] instanceof Class) {
                    return (Class<?>) typeArgs[1]; // retourne type valeur
                }
            }
            return Object.class;
        } else {
            return clazz;
        }
    }

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
