package eu.amadeos.apps.screenshot.ui.main;

import eu.amadeos.commons.i18n.Language;
import eu.amadeos.commons.i18n.LocaleSettings;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.Mnemonics;
import eu.amadeos.commons.swing.EdtExecutor;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.border.BevelBorder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** File, Edit, Options and Help. */
public final class MainMenuBar extends JMenuBar {
    private final Messages msg;

    public MainMenuBar(MainPresenter presenter, ScreenshotActions actions, Messages msg, LocaleSettings locales) {
        EdtExecutor.requireEdt();
        this.msg = msg;
        setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));

        JMenu file = menu("menu.file");
        file.add(item(actions.take));
        file.addSeparator();
        file.add(item(actions.save));
        file.add(item(actions.saveAs));
        file.add(item(actions.openFolder));
        file.addSeparator();
        file.add(item(actions.exit));

        JMenu edit = menu("menu.edit");
        edit.add(item(actions.copy));

        JMenu options = menu("menu.options");
        if (locales != null) options.add(languageMenu(presenter, locales));

        JMenu help = menu("menu.help");
        JMenuItem about = Mnemonics.apply(new JMenuItem(), msg.get("menu.help.about"));
        about.setName("menu.help.about");
        about.addActionListener(e -> presenter.about());
        help.add(about);
    }

    /** "Desktop setting (Deutsch)" and the five languages; the choice applies at the next start. */
    private JMenu languageMenu(MainPresenter presenter, LocaleSettings locales) {
        JMenu menu = Mnemonics.apply(new JMenu(), msg.get("menu.options.language"));
        menu.setName("menu.options.language");
        Messages commons = Messages.commons(msg.locale());
        List<Optional<Language>> choices = new ArrayList<>();
        choices.add(Optional.empty());
        for (Language language : Language.values()) choices.add(Optional.of(language));
        ButtonGroup group = new ButtonGroup();
        Optional<Language> current = locales.applicationLanguage();
        for (Optional<Language> choice : choices) {
            String label = choice.map(Language::nativeName).orElseGet(() -> commons.format("language.follow-desktop",
                    locales.desktopResolution().language().nativeName()));
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(label, choice.equals(current));
            item.setName(choice.map(Language::code).orElse(LocaleSettings.FOLLOW_DESKTOP));
            item.addActionListener(e -> presenter.setLanguage(choice));
            group.add(item);
            menu.add(item);
            if (choice.isEmpty()) menu.addSeparator();
        }
        return menu;
    }

    private JMenu menu(String key) {
        JMenu menu = Mnemonics.apply(new JMenu(), msg.get(key));
        menu.setName(key);
        add(menu);
        return menu;
    }

    private static JMenuItem item(Action action) {
        JMenuItem item = new JMenuItem(action);
        item.setName(String.valueOf(action.getValue(Action.ACTION_COMMAND_KEY)));
        // Menus show text; the small icons would crowd them.
        item.setIcon(null);
        return item;
    }
}
