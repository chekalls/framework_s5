package mg.miniframework.utils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class JsonUtils {

    private static String escapeJson(String s) {
        return s.replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String pojoToJson(Object obj) {
        StringBuilder json = new StringBuilder("{");
        Field[] fields = obj.getClass().getDeclaredFields();

        int count = 0;
        for (Field f : fields) {
            f.setAccessible(true);
            try {
                json.append("\"").append(f.getName()).append("\":")
                        .append(objectToJson(f.get(obj)));
            } catch (IllegalAccessException e) {
                json.append("\"").append(f.getName()).append("\":null");
            }

            if (count < fields.length - 1)
                json.append(",");
            count++;
        }

        json.append("}");
        return json.toString();
    }

    private static String listToJson(List<Object> list) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            json.append(objectToJson(list.get(i)));
            if (i < list.size() - 1)
                json.append(",");
        }
        json.append("]");
        return json.toString();
    }

    public static String objectToJson(Object value) {
        if (value == null) {
            return "null";
        }

        if (value instanceof String) {
            return "\"" + escapeJson((String) value) + "\"";
        }

        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }

        if (value instanceof Map) {
            return mapToJson((Map<String, Object>) value);
        }

        if (value instanceof List) {
            return listToJson((List<Object>) value);
        }

        // Si c’est un objet Java normal → sérialisation via réflexion
        return pojoToJson(value);
    }

    public static String mapToJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");

        int index = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            json.append("\"").append(entry.getKey()).append("\":");
            json.append(objectToJson(entry.getValue()));

            if (index < map.size() - 1)
                json.append(",");
            index++;
        }

        json.append("}");
        return json.toString();
    }

}
