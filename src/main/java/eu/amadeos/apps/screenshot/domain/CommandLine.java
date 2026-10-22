package eu.amadeos.apps.screenshot.domain;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * What the command line asks for. Without arguments the window opens; with a mode, a delay or a target the
 * screenshot is taken at once, without the window, e.g. from a key binding:
 *
 * <pre>
 * amade-screenshot --desktop               saved in the pictures folder
 * amade-screenshot --area --copy           to the clipboard
 * amade-screenshot --window --delay 3 --file /tmp/shot.png
 * </pre>
 */
public sealed interface CommandLine {
    String USAGE = """
            Usage: amade-screenshot [--desktop | --window | --area] [--delay SECONDS]
                                    [--copy] [--save | --file FILE]

            Without options the window opens. With options the screenshot is taken at once:
              --desktop        all screens (the default)
              --window         a window, picked with a click
              --area           an area, drawn with the mouse
              --delay SECONDS  wait first (0 to %d)
              --copy           put the screenshot on the clipboard; the program stays
                               in the background until something else is copied
              --save           save it in the pictures folder (the default)
              --file FILE      save it as FILE (.png or .jpg)
              --help           show this text
              --version        show the version
            """.formatted(Options.MAX_DELAY);

    /** No arguments: the window. */
    record OpenWindow() implements CommandLine {
    }

    record Help() implements CommandLine {
    }

    record Version() implements CommandLine {
    }

    /**
     * Take one screenshot without the window.
     *
     * @param file where to save it; empty with {@code save} for the pictures folder
     */
    record Take(CaptureMode mode, int delay, boolean copy, boolean save, Optional<Path> file) implements CommandLine {
    }

    /** @throws IllegalArgumentException with a message for the terminal */
    static CommandLine parse(List<String> args) {
        if (args.isEmpty()) return new OpenWindow();
        CaptureMode mode = null;
        int delay = 0;
        boolean copy = false;
        boolean save = false;
        Path file = null;
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            switch (arg) {
                case "--help", "-h" -> {
                    return new Help();
                }
                case "--version" -> {
                    return new Version();
                }
                case "--desktop", "--window", "--area" -> {
                    if (mode != null) throw new IllegalArgumentException("Only one of --desktop, --window and --area");
                    mode = CaptureMode.forKey(arg.substring(2)).orElseThrow();
                }
                case "--delay" -> delay = delay(value(args, ++i, arg));
                case "--copy" -> copy = true;
                case "--save" -> save = true;
                case "--file" -> file = Path.of(value(args, ++i, arg));
                default -> throw new IllegalArgumentException("Unknown option " + arg);
            }
        }
        if (save && file != null) throw new IllegalArgumentException("Either --save or --file");
        if (!copy && file == null) save = true;
        return new Take(mode == null ? CaptureMode.DESKTOP : mode, delay, copy, save, Optional.ofNullable(file));
    }

    private static String value(List<String> args, int index, String option) {
        if (index >= args.size() || args.get(index).startsWith("--")) {
            throw new IllegalArgumentException(option + " needs a value");
        }
        return args.get(index);
    }

    private static int delay(String text) {
        try {
            int seconds = Integer.parseInt(text);
            if (seconds >= 0 && seconds <= Options.MAX_DELAY) return seconds;
        } catch (NumberFormatException e) {
            // reported below
        }
        throw new IllegalArgumentException("--delay takes 0 to " + Options.MAX_DELAY + " seconds, not " + text);
    }
}
