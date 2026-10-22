package eu.amadeos.apps.screenshot.bootstrap;

import eu.amadeos.apps.screenshot.domain.CommandLine;
import eu.amadeos.apps.screenshot.ui.main.MainPresenter;
import eu.amadeos.apps.screenshot.ui.main.MainWindow;
import eu.amadeos.commons.app.AmadeApplication;
import eu.amadeos.commons.app.AppInfo;

import java.util.List;

public final class AmadeScreenshotApplication {
    public static final AppInfo APP = new AppInfo("amade-screenshot", "Screenshot", "screenshot");

    private AmadeScreenshotApplication() {
    }

    /** The version from the jar's manifest; "dev" when run from the IDE. */
    public static String version() {
        String version = AmadeScreenshotApplication.class.getPackage().getImplementationVersion();
        return version == null ? "dev" : version;
    }

    // --8<-- [start:main]
    /** Without arguments the window; with options one screenshot at once (see {@link CommandLine}). */
    public static void main(String[] args) {
        CommandLine command;
        try {
            command = CommandLine.parse(List.of(args));
        } catch (IllegalArgumentException e) {
            System.err.println("amade-screenshot: " + e.getMessage());
            System.err.print(CommandLine.USAGE);
            System.exit(2);
            return;
        }
        switch (command) {
            case CommandLine.Help help -> System.out.print(CommandLine.USAGE);
            case CommandLine.Version v -> System.out.println("amade-screenshot " + version());
            case CommandLine.OpenWindow open -> AmadeApplication.launch(APP, injector -> {
                MainWindow window = injector.getInstance(MainWindow.class);
                MainPresenter presenter = injector.getInstance(MainPresenter.class);
                window.bind(presenter);
                window.setVisible(true);
                presenter.start();
            }, new ScreenshotModule());
            case CommandLine.Take take -> AmadeApplication.launch(APP,
                    injector -> injector.getInstance(QuickShot.class).run(take), new ScreenshotModule());
        }
    }
    // --8<-- [end:main]
}
