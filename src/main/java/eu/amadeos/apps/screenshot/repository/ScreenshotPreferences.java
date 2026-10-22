package eu.amadeos.apps.screenshot.repository;

import com.google.inject.Inject;
import eu.amadeos.apps.screenshot.domain.CaptureMode;
import eu.amadeos.apps.screenshot.domain.Options;
import eu.amadeos.commons.config.AppDirectories;
import eu.amadeos.commons.config.PropertiesFile;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

/**
 * Settings of amade-screenshot in {@code ~/.config/amadeos/screenshot/screenshot.properties}: the last
 * mode, delay and whether the window hides. Each change is written at once.
 */
public class ScreenshotPreferences {
    public static final String FILE = "screenshot.properties";

    private final Path file;
    private final Properties properties;

    @Inject
    public ScreenshotPreferences(AppDirectories directories) {
        this(directories.configFile(FILE));
    }

    public ScreenshotPreferences(Path file) {
        this.file = Objects.requireNonNull(file, "file");
        this.properties = PropertiesFile.load(file);
    }

    public Options options() {
        CaptureMode mode = CaptureMode.forKey(properties.getProperty("mode", "")).orElse(Options.DEFAULT.mode());
        int delay = Options.DEFAULT.delay();
        try {
            int value = Integer.parseInt(properties.getProperty("delay", "").strip());
            if (value >= 0 && value <= Options.MAX_DELAY) delay = value;
        } catch (NumberFormatException e) {
            // keep the default
        }
        boolean hide = !"false".equals(properties.getProperty("hide-window"));
        return new Options(mode, delay, hide);
    }

    public void setOptions(Options options) {
        properties.setProperty("mode", options.mode().key());
        properties.setProperty("delay", String.valueOf(options.delay()));
        properties.setProperty("hide-window", String.valueOf(options.hideWindow()));
        PropertiesFile.store(file, properties, "amade-screenshot");
    }
}
