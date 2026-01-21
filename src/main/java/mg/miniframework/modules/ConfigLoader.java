package mg.miniframework.modules;


import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import jakarta.servlet.ServletContext;

public class ConfigLoader {

    public Map<String, String> getAllProperties(ServletContext context) {
        Map<String, String> map = new HashMap<>();

        Set<String> propertyFiles = context.getResourcePaths("/WEB-INF/config/");

        if (propertyFiles != null) {
            for (String path : propertyFiles) {
                if (path.endsWith(".properties")) {
                    try (InputStream in = context.getResourceAsStream(path)) {
                        if (in != null) {
                            Properties props = new Properties();
                            props.load(in);
                            props.forEach((k, v) -> map.put(k.toString(), v.toString()));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return map;
    }
}
