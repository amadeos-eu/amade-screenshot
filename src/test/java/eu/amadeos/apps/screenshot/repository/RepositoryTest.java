package eu.amadeos.apps.screenshot.repository;

import eu.amadeos.apps.screenshot.domain.CaptureMode;
import eu.amadeos.apps.screenshot.domain.ImageFormat;
import eu.amadeos.apps.screenshot.domain.Options;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryTest {
    @TempDir
    Path dir;

    @Test
    void optionsAreRememberedInTheSettingsFile() {
        Path file = dir.resolve("screenshot/screenshot.properties");
        assertEquals(Options.DEFAULT, new ScreenshotPreferences(file).options());
        Options options = new Options(CaptureMode.AREA, 5, false);
        new ScreenshotPreferences(file).setOptions(options);
        assertEquals(options, new ScreenshotPreferences(file).options());
    }

    @Test
    void brokenSettingsFallBackToTheDefaults() throws IOException {
        Path file = dir.resolve("screenshot.properties");
        Files.writeString(file, "mode=everything\ndelay=999\nhide-window=maybe\n");
        assertEquals(Options.DEFAULT, new ScreenshotPreferences(file).options());
        Files.writeString(file, "delay=abc\n");
        assertEquals(Options.DEFAULT, new ScreenshotPreferences(file).options());
    }

    @Test
    void picturesAreWrittenWholeAndInTheirFormat() throws IOException {
        BufferedImage image = new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB);
        image.setRGB(5, 5, Color.RED.getRGB());
        ImageFiles files = new ImageFiles();
        Path png = dir.resolve("new/folder/shot.png");
        files.write(image, png, ImageFormat.PNG);
        BufferedImage read = ImageIO.read(png.toFile());
        assertEquals(40, read.getWidth());
        assertEquals(Color.RED.getRGB(), read.getRGB(5, 5));
        Path jpg = dir.resolve("shot.jpg");
        BufferedImage argb = new BufferedImage(40, 30, BufferedImage.TYPE_INT_ARGB);
        files.write(argb, jpg, ImageFormat.JPEG);
        assertEquals(30, ImageIO.read(jpg.toFile()).getHeight());
        try (Stream<Path> left = Files.list(dir.resolve("new/folder"))) {
            assertEquals(1, left.count(), "no temporary file is left");
        }
        assertTrue(Files.size(jpg) > 0);
    }
}
