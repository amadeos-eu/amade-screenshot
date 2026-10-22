package eu.amadeos.apps.screenshot.bootstrap;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import eu.amadeos.apps.screenshot.repository.ImageFiles;
import eu.amadeos.apps.screenshot.repository.ScreenshotPreferences;
import eu.amadeos.apps.screenshot.service.CaptureService;
import eu.amadeos.apps.screenshot.service.ClipboardHandoff;
import eu.amadeos.apps.screenshot.service.KeepAlive;
import eu.amadeos.apps.screenshot.service.RobotScreen;
import eu.amadeos.apps.screenshot.service.Screen;
import eu.amadeos.apps.screenshot.service.WindowList;
import eu.amadeos.apps.screenshot.service.X11ClipboardHandoff;
import eu.amadeos.apps.screenshot.service.X11WindowList;
import eu.amadeos.apps.screenshot.ui.main.MainPresenter;
import eu.amadeos.apps.screenshot.ui.main.MainWindow;
import eu.amadeos.commons.app.Ui;
import eu.amadeos.commons.config.XdgDirectories;
import eu.amadeos.commons.desktop.DesktopLauncher;
import eu.amadeos.commons.i18n.LocaleSettings;
import eu.amadeos.commons.i18n.Messages;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * amade-screenshot bindings. The background executor, theme, language and the other shared services come
 * from {@link eu.amadeos.commons.app.CommonsModule}.
 */
public final class ScreenshotModule extends AbstractModule {
    /** The texts, {@code messages.properties} and its translations. */
    public static final String MESSAGES = "eu.amadeos.apps.screenshot.messages";

    @Override
    protected void configure() {
        bind(Screen.class).to(RobotScreen.class).in(Scopes.SINGLETON);
        bind(WindowList.class).to(X11WindowList.class).in(Scopes.SINGLETON);
        bind(CaptureService.class).in(Scopes.SINGLETON);
        bind(ImageFiles.class).in(Scopes.SINGLETON);
        bind(ScreenshotPreferences.class).in(Scopes.SINGLETON);
        bind(ClipboardHandoff.class).to(X11ClipboardHandoff.class).in(Scopes.SINGLETON);
        bind(KeepAlive.class).in(Scopes.SINGLETON);
        bind(MainWindow.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    MainPresenter presenter(MainWindow window, Messages msg, LocaleSettings locales, ScreenshotPreferences preferences,
                            CaptureService captures, ImageFiles files, DesktopLauncher launcher,
                            XdgDirectories xdg, ExecutorService background, @Ui Executor ui, KeepAlive keepAlive) {
        return new MainPresenter(window, msg, locales, preferences, captures, files, launcher, xdg.pictures(),
                background, ui, MainPresenter.Pause.SLEEP, LocalDateTime::now, keepAlive, () -> System.exit(0));
    }

    @Provides
    @Singleton
    QuickShot quickShot(CaptureService captures, ImageFiles files, Messages msg, XdgDirectories xdg,
                        ExecutorService background, @Ui Executor ui, KeepAlive keepAlive) {
        return new QuickShot(captures, files, msg, xdg.pictures(), background, ui, MainPresenter.Pause.SLEEP,
                LocalDateTime::now, keepAlive, System.out, System::exit);
    }

    /** The texts in the language {@code AmadeApplication} chose. */
    @Provides
    @Singleton
    Messages messages(Locale locale) {
        return Messages.load(ScreenshotModule.class, MESSAGES, locale);
    }
}
