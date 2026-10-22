package eu.amadeos.apps.screenshot.ui.main;

import eu.amadeos.commons.swing.EdtExecutor;
import eu.amadeos.commons.swing.ToolButtons;

import javax.swing.BorderFactory;
import javax.swing.JToolBar;
import javax.swing.border.BevelBorder;

/** Take; save, copy; open the folder. */
public final class MainToolBar extends JToolBar {
    public MainToolBar(ScreenshotActions actions) {
        EdtExecutor.requireEdt();
        setFloatable(false);
        setRollover(false);
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED),
                BorderFactory.createEmptyBorder(2, 3, 2, 3)));
        add(ToolButtons.button(actions.take));
        addSeparator();
        add(ToolButtons.button(actions.save));
        add(ToolButtons.button(actions.copy));
        addSeparator();
        add(ToolButtons.button(actions.openFolder));
    }
}
