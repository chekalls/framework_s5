package mg.miniframework.modules;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import mg.miniframework.modules.LogManager.LogStatus;

public class File {
    private Path contextPath;
    private Path absolutePath;
    private byte[] content;
    private LogManager logManager;

    public void save() {
        if (absolutePath == null) {
            throw new IllegalStateException("absolutePath is not defined");
        }
        if (content == null) {
            throw new IllegalStateException("content is null");
        }

        try {
            Path parent = absolutePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Path tempFile = Files.createTempFile(parent, "upload_", ".tmp");
            Files.write(tempFile, content);
            if(logManager!=null){
                logManager.insertLog("saving file into :"+absolutePath.toString(), LogStatus.DEBUG);
            }

            Files.move(
                    tempFile,
                    absolutePath,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);

        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Unable to save file to " + absolutePath,
                    e);
        }
    }

    public Path getContextPath() {
        return contextPath;
    }

    public void setContextPath(Path contextPath) {
        this.contextPath = contextPath;
    }

    public Path getAbsolutePath() {
        return absolutePath;
    }

    public void setAbsolutePath(Path absolutePath) {
        this.absolutePath = absolutePath;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public LogManager getLogManager() {
        return logManager;
    }

    public void setLogManager(LogManager logManager) {
        this.logManager = logManager;
    }
}
