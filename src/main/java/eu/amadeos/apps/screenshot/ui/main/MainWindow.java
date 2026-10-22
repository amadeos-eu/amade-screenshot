package eu.amadeos.apps.screenshot.ui.main;

import com.google.inject.Inject;
import eu.amadeos.apps.screenshot.bootstrap.AmadeScreenshotApplication;
import eu.amadeos.apps.screenshot.domain.CaptureMode;
import eu.amadeos.apps.screenshot.domain.ImageFormat;
import eu.amadeos.apps.screenshot.domain.Options;
import eu.amadeos.apps.screenshot.service.Capture;
import eu.amadeos.apps.screenshot.ui.icons.ScreenshotGlyph;
import eu.amadeos.apps.screenshot.ui.select.SelectionOverlay;
import eu.amadeos.commons.desktop.ImageSelection;
import eu.amadeos.commons.i18n.LocaleSettings;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.Mnemonics;
import eu.amadeos.commons.swing.EdtExecutor;
import eu.amadeos.commons.swing.ToolIcon;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.border.BevelBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The window: toolbar, what to take on the left (desktop, window or area, the delay, whether the window
 * hides), the last screenshot on the right and a status bar.
 */
public final class MainWindow extends JFrame implements MainView {
    private final Messages msg;
    private final LocaleSettings locales;
    private final ShotPreview preview = new ShotPreview();
    private final JLabel status = new JLabel(" ");
    private final Map<CaptureMode, JRadioButton> modes = new EnumMap<>(CaptureMode.class);
    private final JComboBox<Integer> delay = new JComboBox<>();
    private final JCheckBox hide;
    private final JPanel center = new JPanel(new BorderLayout(6, 6));
    private ScreenshotActions actions;
    private MainPresenter presenter;
    private boolean updating;

    @Inject
    public MainWindow(Messages msg, LocaleSettings locales) {
        super(msg.get("window.title"));
        EdtExecutor.requireEdt();
        this.msg = msg;
        this.locales = locales;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        hide = Mnemonics.apply(new JCheckBox(), msg.get("take.hide"));
        hide.setName("take.hide");
        preview.setPlaceholder(msg.get("preview.empty"));
        JPanel desk = new JPanel(new BorderLayout());
        desk.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        desk.add(preview, BorderLayout.CENTER);
        center.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        center.add(desk, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
        add(statusBar(), BorderLayout.SOUTH);
        java.net.URL icon = getClass().getResource("/icons/amade-screenshot.png");
        if (icon != null) setIconImage(new javax.swing.ImageIcon(icon).getImage());
        setPreferredSize(new Dimension(820, 460));
        pack();
        setLocationRelativeTo(null);
    }

    public void bind(MainPresenter newPresenter) {
        EdtExecutor.requireEdt();
        presenter = newPresenter;
        actions = new ScreenshotActions(presenter, msg);
        setJMenuBar(new MainMenuBar(presenter, actions, msg, locales));
        add(new MainToolBar(actions), BorderLayout.NORTH);
        center.add(optionsPanel(), BorderLayout.WEST);
        for (Action a : actions.all()) {
            KeyStroke key = (KeyStroke) a.getValue(Action.ACCELERATOR_KEY);
            if (key == null) continue;
            String name = a.getValue(Action.ACTION_COMMAND_KEY) + "@" + key;
            getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, name);
            getRootPane().getActionMap().put(name, a);
        }
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                presenter.exit();
            }
        });
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel optionsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setName("options");
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), msg.get("take.title")),
                BorderFactory.createEmptyBorder(4, 6, 6, 6)));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(2, 0, 2, 0);
        ButtonGroup group = new ButtonGroup();
        for (CaptureMode mode : CaptureMode.values()) {
            JRadioButton button = Mnemonics.apply(new JRadioButton(), msg.get(modeKey(mode)));
            button.setName(modeKey(mode));
            button.addActionListener(e -> {
                if (!updating) presenter.setMode(mode);
            });
            group.add(button);
            modes.put(mode, button);
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            row.add(button);
            row.add(new JLabel(new ToolIcon(modeGlyph(mode), 16)));
            panel.add(row, c);
            c.gridy++;
        }
        c.gridwidth = 1;
        c.insets = new Insets(10, 4, 2, 6);
        JLabel delayLabel = Mnemonics.apply(new JLabel(), msg.get("take.delay"));
        delayLabel.setLabelFor(delay);
        panel.add(delayLabel, c);
        c.gridx = 1;
        c.insets = new Insets(10, 0, 2, 0);
        delay.setName("take.delay");
        for (int seconds : Options.DELAYS) delay.addItem(seconds);
        delay.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
                                                          boolean focus) {
                String text = value instanceof Integer s ? delayText(s) : "";
                return super.getListCellRendererComponent(list, text, index, selected, focus);
            }
        });
        delay.addActionListener(e -> {
            if (!updating && delay.getSelectedItem() instanceof Integer s) presenter.setDelay(s);
        });
        panel.add(delay, c);
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.insets = new Insets(4, 0, 2, 0);
        hide.addActionListener(e -> {
            if (!updating) presenter.setHideWindow(hide.isSelected());
        });
        panel.add(hide, c);
        c.gridy++;
        c.insets = new Insets(14, 0, 0, 0);
        c.anchor = GridBagConstraints.CENTER;
        JButton take = new JButton(actions.take);
        take.setName("take.button");
        take.setToolTipText(null);
        panel.add(take, c);
        c.gridy++;
        c.weighty = 1;
        panel.add(new JLabel(), c);
        return panel;
    }

    String delayText(int seconds) {
        return seconds == 0 ? msg.get("take.delay.none") : msg.format("take.delay.seconds", seconds);
    }

    static String modeKey(CaptureMode mode) {
        return "mode." + mode.key();
    }

    private static ScreenshotGlyph modeGlyph(CaptureMode mode) {
        return switch (mode) {
            case DESKTOP -> ScreenshotGlyph.DESKTOP;
            case WINDOW -> ScreenshotGlyph.WINDOW;
            case AREA -> ScreenshotGlyph.AREA;
        };
    }

    public ScreenshotActions actions() {
        return actions;
    }

    ShotPreview preview() {
        return preview;
    }

    String statusText() {
        return status.getText();
    }

    JRadioButton modeButton(CaptureMode mode) {
        return modes.get(mode);
    }

    JComboBox<Integer> delayBox() {
        return delay;
    }

    JCheckBox hideBox() {
        return hide;
    }

    // ---------------------------------------------------------------- MainView

    @Override
    public void showOptions(Options options) {
        EdtExecutor.requireEdt();
        updating = true;
        try {
            modes.get(options.mode()).setSelected(true);
            if (((javax.swing.DefaultComboBoxModel<Integer>) delay.getModel()).getIndexOf(options.delay()) < 0) {
                delay.addItem(options.delay());
            }
            delay.setSelectedItem(options.delay());
            hide.setSelected(options.hideWindow());
        } finally {
            updating = false;
        }
    }

    @Override
    public void showShot(BufferedImage shot) {
        EdtExecutor.requireEdt();
        preview.setShot(shot);
    }

    @Override
    public void showStatus(String text) {
        EdtExecutor.requireEdt();
        status.setText(text == null || text.isEmpty() ? " " : text);
    }

    @Override
    public void updateCommands(Commands commands) {
        EdtExecutor.requireEdt();
        if (actions != null) actions.update(commands);
    }

    // --8<-- [start:hide]
    @Override
    public void hideForCapture() {
        EdtExecutor.requireEdt();
        setVisible(false);
    }

    @Override
    public void showAfterCapture() {
        EdtExecutor.requireEdt();
        if (!isVisible()) setVisible(true);
        toFront();
    }

    @Override
    public void select(CaptureMode mode, Capture capture, Consumer<Optional<Rectangle>> done) {
        EdtExecutor.requireEdt();
        SelectionOverlay.show(capture, mode, msg.get("select." + mode.key()), done);
    }
    // --8<-- [end:hide]

    @Override
    public Optional<SaveTarget> chooseSavePath(Path folder, String suggestedName) {
        EdtExecutor.requireEdt();
        JFileChooser chooser = new JFileChooser(Files.isDirectory(folder) ? folder.toFile() : null);
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        Map<FileFilter, ImageFormat> byFilter = new LinkedHashMap<>();
        for (ImageFormat format : ImageFormat.values()) {
            String patterns = String.join(", ", format.extensions().stream().map(e -> "*." + e).toList());
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    msg.format("filter.format", msg.get("format." + format.defaultExtension()), patterns),
                    format.extensions().toArray(String[]::new));
            byFilter.put(filter, format);
            chooser.addChoosableFileFilter(filter);
            if (format == ImageFormat.PNG) chooser.setFileFilter(filter);
        }
        chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), suggestedName));
        // Switching the type changes the extension of the name.
        chooser.addPropertyChangeListener(JFileChooser.FILE_FILTER_CHANGED_PROPERTY, e -> {
            ImageFormat format = byFilter.get(chooser.getFileFilter());
            File current = chooser.getSelectedFile();
            if (format != null && current != null && !current.getName().isBlank()) {
                chooser.setSelectedFile(format.withExtension(new File(chooser.getCurrentDirectory(),
                        current.getName()).toPath()).toFile());
            }
        });
        while (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            ImageFormat format = byFilter.getOrDefault(chooser.getFileFilter(), ImageFormat.PNG);
            Path path = chooser.getSelectedFile().toPath();
            Optional<ImageFormat> typed = ImageFormat.forPath(path);
            if (typed.isPresent()) format = typed.get();
            else path = format.withExtension(path);
            if (!Files.exists(path) || confirmOverwrite(path)) return Optional.of(new SaveTarget(path, format));
        }
        return Optional.empty();
    }

    private boolean confirmOverwrite(Path path) {
        return JOptionPane.showConfirmDialog(this, plain(msg.format("dialog.overwrite.message", path.getFileName())),
                msg.get("dialog.overwrite.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)
                == JOptionPane.YES_OPTION;
    }

    @Override
    public void copyToClipboard(BufferedImage picture, Runnable lost) {
        EdtExecutor.requireEdt();
        ImageSelection.put(Toolkit.getDefaultToolkit().getSystemClipboard(), picture, lost);
    }

    @Override
    public void showError(String message) {
        EdtExecutor.requireEdt();
        JOptionPane.showMessageDialog(this, plain(message), msg.get("dialog.error.title"), JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void showInfo(String message) {
        EdtExecutor.requireEdt();
        JOptionPane.showMessageDialog(this, plain(message), getTitle(), JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void showAbout() {
        EdtExecutor.requireEdt();
        JOptionPane.showMessageDialog(this,
                "<html><center><h2>amade-screenshot " + AmadeScreenshotApplication.version() + "</h2>"
                        + "<p>" + msg.get("dialog.about.text") + "</p><br>"
                        + "<small>&copy; 2026 datazuul.com &bull; EUPL-1.2</small></center></html>",
                msg.get("dialog.about.title"), JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void close() {
        EdtExecutor.requireEdt();
        dispose();
    }

    /** A text component, so that messages (with file names and errors) are not read as Swing HTML. */
    private static JTextArea plain(String message) {
        JTextArea text = new JTextArea(message);
        text.setEditable(false);
        text.setOpaque(false);
        text.setLineWrap(message.length() > 200);
        text.setWrapStyleWord(true);
        if (message.length() > 200) text.setColumns(50);
        return text;
    }

    private JPanel statusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        bar.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        status.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        status.setName("status");
        bar.add(status);
        return bar;
    }
}
