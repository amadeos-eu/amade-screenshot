package eu.amadeos.apps.screenshot.ui.main;

import eu.amadeos.apps.screenshot.domain.CaptureMode;
import eu.amadeos.apps.screenshot.domain.Options;
import eu.amadeos.apps.screenshot.service.Capture;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/** Records what the presenter shows and answers its questions as set up by the test. */
final class FakeView implements MainView {
    final List<String> statuses = new ArrayList<>();
    final List<String> errors = new ArrayList<>();
    final List<String> infos = new ArrayList<>();
    final List<String> events = new ArrayList<>();
    Options options;
    BufferedImage shot;
    Commands commands = Commands.NONE;
    boolean visible = true;
    boolean closed;
    /** What the user picks on the overlay; empty cancels. */
    Optional<Rectangle> selection = Optional.empty();
    CaptureMode selectedIn;
    Optional<SaveTarget> saveTarget = Optional.empty();
    Path chooserFolder;
    String suggestedName;
    BufferedImage clipboard;
    Runnable clipboardLost;
    int abouts;

    String lastStatus() {
        return statuses.isEmpty() ? null : statuses.getLast();
    }

    @Override
    public void showOptions(Options newOptions) {
        options = newOptions;
    }

    @Override
    public void showShot(BufferedImage newShot) {
        shot = newShot;
    }

    @Override
    public void showStatus(String text) {
        statuses.add(text);
    }

    @Override
    public void updateCommands(Commands newCommands) {
        commands = newCommands;
    }

    @Override
    public void hideForCapture() {
        visible = false;
        events.add("hide");
    }

    @Override
    public void showAfterCapture() {
        visible = true;
        events.add("show");
    }

    @Override
    public void select(CaptureMode mode, Capture capture, Consumer<Optional<Rectangle>> done) {
        selectedIn = mode;
        events.add("select");
        done.accept(selection);
    }

    @Override
    public Optional<SaveTarget> chooseSavePath(Path folder, String name) {
        chooserFolder = folder;
        suggestedName = name;
        return saveTarget;
    }

    @Override
    public void copyToClipboard(BufferedImage picture, Runnable lost) {
        clipboard = picture;
        clipboardLost = lost;
    }

    @Override
    public void showError(String message) {
        errors.add(message);
    }

    @Override
    public void showInfo(String message) {
        infos.add(message);
    }

    @Override
    public void showAbout() {
        abouts++;
    }

    @Override
    public void close() {
        closed = true;
    }
}
