package eu.amadeos.apps.screenshot.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * The user's pictures folder: {@code XDG_PICTURES_DIR} from {@code ~/.config/user-dirs.dirs} (translated
 * names such as {@code ~/Bilder}), else {@code ~/Pictures}. It is created when the first screenshot is saved.
 */
public final class PicturesFolder {
    private PicturesFolder() {
    }

    public static Path of(String userHome, Map<String, String> environment) {
        Path home = Path.of(userHome);
        String configHome = environment.get("XDG_CONFIG_HOME");
        Path config = configHome != null && configHome.startsWith("/") ? Path.of(configHome) : home.resolve(".config");
        Path dirs = config.resolve("user-dirs.dirs");
        try {
            if (Files.isRegularFile(dirs)) {
                for (String line : Files.readAllLines(dirs)) {
                    line = line.strip();
                    if (!line.startsWith("XDG_PICTURES_DIR=")) continue;
                    String value = line.substring(line.indexOf('=') + 1).replace("\"", "");
                    if (value.startsWith("$HOME")) return home.resolve(value.substring(5).replaceFirst("^/+", ""));
                    if (value.startsWith("/")) return Path.of(value);
                }
            }
        } catch (IOException | RuntimeException e) {
            // fall back to ~/Pictures
        }
        return home.resolve("Pictures");
    }

    /** The folder of the running user. */
    public static Path system() {
        return of(System.getProperty("user.home"), System.getenv());
    }
}
