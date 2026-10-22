package eu.amadeos.apps.screenshot.domain;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** The file types a screenshot can be saved as. PNG keeps every pixel; JPEG is smaller for photos. */
public enum ImageFormat {
    PNG("png", List.of("png")),
    JPEG("jpeg", List.of("jpg", "jpeg"));

    private final String writerName;
    private final List<String> extensions;

    ImageFormat(String writerName, List<String> extensions) {
        this.writerName = writerName;
        this.extensions = extensions;
    }

    /** The name ImageIO knows the writer by. */
    public String writerName() {
        return writerName;
    }

    public List<String> extensions() {
        return extensions;
    }

    public String defaultExtension() {
        return extensions.getFirst();
    }

    /** The format the file name's extension stands for, if any. */
    public static Optional<ImageFormat> forPath(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0) return Optional.empty();
        String extension = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        for (ImageFormat format : values()) {
            if (format.extensions.contains(extension)) return Optional.of(format);
        }
        return Optional.empty();
    }

    /** The path with this format's extension: added, or in place of another image extension. */
    public Path withExtension(Path path) {
        String name = path.getFileName().toString();
        if (forPath(path).isPresent()) name = name.substring(0, name.lastIndexOf('.'));
        return path.resolveSibling(name + "." + defaultExtension());
    }
}
