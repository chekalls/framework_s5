package mg.miniframework.modules;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogManager {

    private static String logBasePath = "/home/itu/Documents/s5/framework/projet/framework_s5/log";

    public LogManager() {
        File folder = new File(logBasePath);
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public void insertLog(String message, LogStatus status) throws IOException {
        String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String logFileName = "log_" + dateStr + ".txt";

        File logFolder = new File(logBasePath);
        if (!logFolder.exists()) {
            logFolder.mkdirs();
        }

        File logFile = new File(logFolder, logFileName);
        if (!logFile.exists()) {
            logFile.createNewFile();
        }

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String logEntry = String.format("[%s] [%s] %s%n", timestamp, status.getName(), message);

        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.write(logEntry);
        }
    }

    public static enum LogStatus {
        ERROR("ERROR"),
        INFO("INFO"),
        DEBUG("DEBUG"),
        WARN("WARN"),
        SUCCESS("SUCCESS");

        private String name;

        private LogStatus(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }
}
