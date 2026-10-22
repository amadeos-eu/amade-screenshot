package eu.amadeos.apps.screenshot.ui.main;

import eu.amadeos.apps.screenshot.ui.icons.ScreenshotGlyph;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.Mnemonics;
import eu.amadeos.commons.swing.StandardGlyph;
import eu.amadeos.commons.swing.ToolIcon;
import eu.amadeos.commons.swing.ToolIcon.Glyph;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * One action per command, shared by the menus, the toolbar and the buttons, so that they are enabled and
 * disabled together. Texts come from the messages ({@code &} marks the mnemonic), icons from
 * {@link ToolIcon}.
 */
public final class ScreenshotActions {
    private static final int CTRL = InputEvent.CTRL_DOWN_MASK;
    private static final int SHIFT = InputEvent.SHIFT_DOWN_MASK;

    public final Action take;
    public final Action save;
    public final Action saveAs;
    public final Action openFolder;
    public final Action exit;
    public final Action copy;

    private final Messages msg;

    public ScreenshotActions(MainPresenter presenter, Messages msg) {
        this.msg = msg;
        take = action("menu.file.take", ScreenshotGlyph.CAMERA, KeyEvent.VK_N, CTRL, presenter::take);
        save = action("menu.file.save", StandardGlyph.SAVE, KeyEvent.VK_S, CTRL, presenter::save);
        saveAs = action("menu.file.save-as", null, KeyEvent.VK_S, CTRL | SHIFT, presenter::saveAs);
        openFolder = action("menu.file.open-folder", ScreenshotGlyph.FOLDER, 0, 0, presenter::openFolder);
        exit = action("menu.file.exit", null, KeyEvent.VK_Q, CTRL, presenter::exit);
        copy = action("menu.edit.copy", StandardGlyph.COPY, KeyEvent.VK_C, CTRL, presenter::copy);
        update(MainView.Commands.NONE);
    }

    /** All actions, for tests and key bindings. */
    public List<Action> all() {
        return List.of(take, save, saveAs, openFolder, exit, copy);
    }

    public void update(MainView.Commands c) {
        take.setEnabled(!c.taking());
        save.setEnabled(c.hasShot() && !c.taking());
        saveAs.setEnabled(c.hasShot() && !c.taking());
        copy.setEnabled(c.hasShot() && !c.taking());
        openFolder.setEnabled(c.hasSavedFile());
    }

    private Action action(String key, Glyph glyph, int keyCode, int modifiers, Runnable command) {
        Mnemonics.Text text = Mnemonics.parse(msg.get(key));
        Action action = new AbstractAction(text.text()) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isEnabled()) command.run();
            }
        };
        action.putValue(Action.ACTION_COMMAND_KEY, key);
        if (text.index() >= 0) {
            action.putValue(Action.MNEMONIC_KEY, text.keyCode());
            action.putValue(Action.DISPLAYED_MNEMONIC_INDEX_KEY, text.index());
        }
        String tip = msg.has(key + ".tip") ? msg.get(key + ".tip") : text.text();
        action.putValue(Action.SHORT_DESCRIPTION, tip);
        if (keyCode != 0) action.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(keyCode, modifiers));
        if (glyph != null) {
            action.putValue(Action.SMALL_ICON, new ToolIcon(glyph, 16));
            action.putValue(Action.LARGE_ICON_KEY, new ToolIcon(glyph));
        }
        return action;
    }
}
