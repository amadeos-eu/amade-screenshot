package eu.amadeos.apps.screenshot.domain;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandLineTest {
    private static CommandLine parse(String... args) {
        return CommandLine.parse(List.of(args));
    }

    @Test
    void withoutArgumentsTheWindowOpens() {
        assertInstanceOf(CommandLine.OpenWindow.class, parse());
    }

    @Test
    void helpAndVersion() {
        assertInstanceOf(CommandLine.Help.class, parse("--help"));
        assertInstanceOf(CommandLine.Help.class, parse("--area", "-h"));
        assertInstanceOf(CommandLine.Version.class, parse("--version"));
        assertTrue(CommandLine.USAGE.contains("--delay SECONDS"));
    }

    @Test
    void aModeAloneSavesInThePicturesFolder() {
        assertEquals(new CommandLine.Take(CaptureMode.WINDOW, 0, false, true, Optional.empty()), parse("--window"));
        assertEquals(new CommandLine.Take(CaptureMode.DESKTOP, 0, false, true, Optional.empty()), parse("--desktop"));
    }

    @Test
    void withoutAModeTheDesktopIsTaken() {
        assertEquals(new CommandLine.Take(CaptureMode.DESKTOP, 3, false, true, Optional.empty()), parse("--delay", "3"));
    }

    @Test
    void copyAloneDoesNotSave() {
        assertEquals(new CommandLine.Take(CaptureMode.AREA, 0, true, false, Optional.empty()), parse("--area", "--copy"));
        assertEquals(new CommandLine.Take(CaptureMode.AREA, 0, true, true, Optional.empty()),
                parse("--area", "--copy", "--save"));
    }

    @Test
    void aFileIsSavedThere() {
        assertEquals(new CommandLine.Take(CaptureMode.WINDOW, 5, false, false, Optional.of(Path.of("/tmp/a.png"))),
                parse("--window", "--delay", "5", "--file", "/tmp/a.png"));
    }

    @Test
    void mistakesAreExplained() {
        assertEquals("Unknown option --full",
                assertThrows(IllegalArgumentException.class, () -> parse("--full")).getMessage());
        assertEquals("Only one of --desktop, --window and --area",
                assertThrows(IllegalArgumentException.class, () -> parse("--area", "--window")).getMessage());
        assertEquals("--delay needs a value",
                assertThrows(IllegalArgumentException.class, () -> parse("--delay")).getMessage());
        assertEquals("--file needs a value",
                assertThrows(IllegalArgumentException.class, () -> parse("--file", "--copy")).getMessage());
        assertEquals("--delay takes 0 to 60 seconds, not 61",
                assertThrows(IllegalArgumentException.class, () -> parse("--delay", "61")).getMessage());
        assertEquals("--delay takes 0 to 60 seconds, not soon",
                assertThrows(IllegalArgumentException.class, () -> parse("--delay", "soon")).getMessage());
        assertEquals("Either --save or --file",
                assertThrows(IllegalArgumentException.class, () -> parse("--save", "--file", "a.png")).getMessage());
    }
}
