package eu.amadeos.apps.screenshot.domain;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Names for new screenshots: {@code Screenshot 2026-10-22 14-05-33.png}. The date sorts by name, and there
 * are no colons, which some file systems and file managers do not like. A second screenshot in the same
 * second gets {@code (2)}, {@code (3)}, ...
 */
public final class FileNames {
    public static final String PREFIX = "Screenshot";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss");

    private FileNames() {
    }

    public static String name(LocalDateTime time, ImageFormat format) {
        return PREFIX + " " + STAMP.format(time) + "." + format.defaultExtension();
    }

    /** A file in the folder that does not exist yet. */
    public static Path unused(Path folder, LocalDateTime time, ImageFormat format) {
        String base = PREFIX + " " + STAMP.format(time);
        Path path = folder.resolve(base + "." + format.defaultExtension());
        for (int n = 2; Files.exists(path); n++) {
            path = folder.resolve(base + " (" + n + ")." + format.defaultExtension());
        }
        return path;
    }
}
