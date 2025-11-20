package mg.miniframework.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RoutePatternUtils {
    public static Pattern convertRouteToPattern(String route) {
        String regex = route.replaceAll("\\{[^/]+\\}", "([^/]+)");
        regex = "^" + regex + "$";
        return Pattern.compile(regex);
    }

    public static Map<String, String> extractPathParams(String pattern, String url) {
        List<String> names = new ArrayList<>();
        Matcher nameMatcher = Pattern.compile("\\{([^/]+)}").matcher(pattern);
        while (nameMatcher.find()) {
            names.add(nameMatcher.group(1));
        }

        String regex = pattern.replaceAll("\\{[^/]+}", "([^/]+)");

        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(url);

        Map<String, String> params = new LinkedHashMap<>();

        if (m.matches()) {
            for (int i = 0; i < names.size(); i++) {
                params.put(names.get(i), m.group(i + 1));
            }
        }

        return params;
    }
}
