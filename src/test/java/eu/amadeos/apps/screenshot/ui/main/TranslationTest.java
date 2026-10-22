package eu.amadeos.apps.screenshot.ui.main;

import eu.amadeos.apps.screenshot.bootstrap.ScreenshotModule;
import eu.amadeos.apps.screenshot.domain.CaptureMode;
import eu.amadeos.commons.i18n.Language;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.Mnemonics;
import eu.amadeos.commons.i18n.TranslationCheck;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslationTest {
    @Test
    void everyLanguageHasEveryText() {
        assertEquals(Map.of(), TranslationCheck.problems(ScreenshotModule.class, ScreenshotModule.MESSAGES));
    }

    @Test
    void everyModeHasATextAndAHint() {
        for (CaptureMode mode : CaptureMode.values()) {
            assertFalse(MainPresenterTest.MSG.get(MainWindow.modeKey(mode)).startsWith("!"));
            if (mode.needsSelection()) assertFalse(MainPresenterTest.MSG.get("select." + mode.key()).startsWith("!"));
        }
    }

    @Test
    void textsAreFormattedInTheLanguage() {
        Messages german = Messages.load(ScreenshotModule.class, ScreenshotModule.MESSAGES, Locale.GERMANY);
        // No thousands separator in pixel counts.
        assertEquals("Bildschirmfoto mit 1920 × 1080 Pixeln.", german.format("status.taken", 1920, 1080));
        Messages french = Messages.load(ScreenshotModule.class, ScreenshotModule.MESSAGES, Locale.FRANCE);
        assertEquals("Impossible d'enregistrer a.png : b", french.format("error.save", "a.png", "b"));
        Messages italian = Messages.load(ScreenshotModule.class, ScreenshotModule.MESSAGES, Locale.ITALY);
        assertEquals("Un'area", Mnemonics.strip(italian.get("mode.area")));
    }

    /**
     * No two entries share a mnemonic letter: within the menu bar, within the File menu, and in the window,
     * where the menu bar's letters, the options and the Take button are all reachable at once.
     */
    @Test
    void mnemonicsAreUnique() {
        List<String> bar = List.of("menu.file", "menu.edit", "menu.options", "menu.help");
        List<String> window = new ArrayList<>(bar);
        for (CaptureMode mode : CaptureMode.values()) window.add(MainWindow.modeKey(mode));
        window.addAll(List.of("take.delay", "take.hide", "menu.file.take"));
        Map<String, List<String>> groups = Map.of(
                "window", window,
                "file", List.of("menu.file.take", "menu.file.save", "menu.file.save-as", "menu.file.open-folder",
                        "menu.file.exit"));
        for (Language language : Language.values()) {
            Messages texts = Messages.load(ScreenshotModule.class, ScreenshotModule.MESSAGES,
                    Locale.forLanguageTag(language.code()));
            for (Map.Entry<String, List<String>> group : groups.entrySet()) {
                Map<Integer, String> used = new HashMap<>();
                for (String key : group.getValue()) {
                    Mnemonics.Text text = Mnemonics.parse(texts.get(key));
                    assertTrue(text.index() >= 0, language + " " + key + " has no mnemonic");
                    String before = used.put(text.keyCode(), key);
                    assertEquals(null, before, language + " " + group.getKey() + ": " + key + " and " + before
                            + " share a letter");
                }
            }
        }
    }
}
