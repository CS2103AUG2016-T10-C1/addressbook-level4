package seedu.simply;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import com.google.common.eventbus.Subscribe;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import seedu.simply.commons.core.Config;
import seedu.simply.commons.core.EventsCenter;
import seedu.simply.commons.core.LogsCenter;
import seedu.simply.commons.core.Version;
import seedu.simply.commons.events.ui.ExitAppRequestEvent;
import seedu.simply.commons.exceptions.DataConversionException;
import seedu.simply.commons.util.ConfigUtil;
import seedu.simply.commons.util.StringUtil;
import seedu.simply.logic.Logic;
import seedu.simply.logic.LogicManager;
import seedu.simply.model.Model;
import seedu.simply.model.ModelManager;
import seedu.simply.model.ReadOnlyTaskBook;
import seedu.simply.model.TaskBook;
import seedu.simply.model.UserPrefs;
import seedu.simply.storage.Storage;
import seedu.simply.storage.StorageManager;
import seedu.simply.ui.Ui;
import seedu.simply.ui.UiManager;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * The main entry point to the application.
 */
public class MainApp extends Application {
    private static final Logger logger = LogsCenter.getLogger(MainApp.class);

    public static final Version VERSION = new Version(1, 0, 0, true);

    protected Ui ui;
    protected Logic logic;
    protected Storage storage;
    protected Model model;
    protected Config config;
    protected UserPrefs userPrefs;

    public MainApp() {}

    @Override
    public void init() throws Exception {
        logger.info("=============================[ Initializing Simply ]===========================");
        super.init();

        config = initConfig(getApplicationParameter("config"));
        storage = new StorageManager(config.getTaskBookFilePath(), config.getUserPrefsFilePath());

        userPrefs = initPrefs(config);

        initLogging(config);

        model = initModelManager(storage, userPrefs, config);

        logic = new LogicManager(model); //, storage, config);

        ui = new UiManager(logic, config, userPrefs);

        initEventsCenter();
    }

    private String getApplicationParameter(String parameterName){
        Map<String, String> applicationParameters = getParameters().getNamed();
        return applicationParameters.get(parameterName);
    }

    private Model initModelManager(Storage storage, UserPrefs userPrefs, Config config) {
        Optional<ReadOnlyTaskBook> addressBookOptional;
        ReadOnlyTaskBook initialData;
        try {
            addressBookOptional = storage.readAddressBook();
            if(!addressBookOptional.isPresent()){
                logger.info("Data file not found. Will be starting with an empty AddressBook");
            }
            initialData = addressBookOptional.orElse(new TaskBook());
        } catch (DataConversionException e) {
            logger.warning("Data file not in the correct format. Will be starting with an empty AddressBook");
            initialData = new TaskBook();
        } catch (IOException e) {
            logger.warning("Problem while reading from the file. . Will be starting with an empty AddressBook");
            initialData = new TaskBook();
        }

        return new ModelManager(initialData, userPrefs, config);
    }

    private void initLogging(Config config) {
        LogsCenter.init(config);
    }

    protected Config initConfig(String configFilePath) {
        Config initializedConfig;
        String configFilePathUsed;

        // Important line here
        configFilePathUsed = Config.DEFAULT_CONFIG_FILE;

        if(configFilePath != null) {
            logger.info("Custom Config file specified " + configFilePath);
            configFilePathUsed = configFilePath;
        }

        logger.info("Using config file : " + configFilePathUsed);

        try {
            Optional<Config> configOptional = ConfigUtil.readConfig(configFilePathUsed);
            initializedConfig = configOptional.orElse(new Config());
        } catch (DataConversionException e) {
            logger.warning("Config file at " + configFilePathUsed + " is not in the correct format. " +
                    "Using default config properties");
            initializedConfig = new Config();
        }

        //Update config file in case it was missing to begin with or there are new/unused fields
        try {
            ConfigUtil.saveConfig(initializedConfig, configFilePathUsed);
        } catch (IOException e) {
            logger.warning("Failed to save config file : " + StringUtil.getDetails(e));
        }
        return initializedConfig;
    }

    protected String getTaskBookFilePath(String configFilePath) {
        Config currentConfig;
        String currentAddressBookFilePath;

        currentConfig = initConfig(configFilePath);
        currentAddressBookFilePath = currentConfig.getTaskBookFilePath();
        return currentAddressBookFilePath;
    }

    protected UserPrefs initPrefs(Config config) {
        assert config != null;

        String prefsFilePath = config.getUserPrefsFilePath();
        logger.info("Using prefs file : " + prefsFilePath);

        UserPrefs initializedPrefs;
        try {
            Optional<UserPrefs> prefsOptional = storage.readUserPrefs();
            initializedPrefs = prefsOptional.orElse(new UserPrefs());
        } catch (DataConversionException e) {
            logger.warning("UserPrefs file at " + prefsFilePath + " is not in the correct format. " +
                    "Using default user prefs");
            initializedPrefs = new UserPrefs();
        } catch (IOException e) {
            logger.warning("Problem while reading from the file. . Will be starting with an empty AddressBook");
            initializedPrefs = new UserPrefs();
        }

        //Update prefs file in case it was missing to begin with or there are new/unused fields
        try {
            storage.saveUserPrefs(initializedPrefs);
        } catch (IOException e) {
            logger.warning("Failed to save config file : " + StringUtil.getDetails(e));
        }

        return initializedPrefs;
    }

    private void initEventsCenter() {
        EventsCenter.getInstance().registerHandler(this);
    }

    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting AddressBook " + MainApp.VERSION);
        ui.start(primaryStage);
    }

    //@@author A0147890U
    @Override
    public void stop() {
        logger.info("============================ [ Stopping Address Book ] =============================");
        ui.stop();
        try {
            storage.saveUserPrefs(userPrefs);
        } catch (IOException e) {
            logger.severe("Failed to save preferences " + StringUtil.getDetails(e));
        }

        Path sourceFilePath = getSourceFilePath();        
        Path targetFilePath = getTargetFilePath();

        assert sourceFilePath != null;
        assert targetFilePath != null;

        try {
            Files.move(sourceFilePath, targetFilePath, REPLACE_EXISTING);
        } catch (IOException e) {
            logger.warning("Failed to move file. Please check if file paths are valid.");
        }

        Platform.exit();
        System.exit(0);
    }

    //@@author A0147890U
    private Path getSourceFilePath() {
        String sourceFile = storage.getAddressBookFilePath();
        Path sourceFilePath = Paths.get(sourceFile);
        return sourceFilePath;
    }

    //@@author A0147890U
    private Path getTargetFilePath() {
        config = initConfig(getApplicationParameter("config"));
        Path targetFilePath = Paths.get(config.getTaskBookFilePath());
        return targetFilePath;
    }

    //@@author
    @Subscribe
    public void handleExitAppRequestEvent(ExitAppRequestEvent event) {
        logger.info(LogsCenter.getEventHandlingLogMessage(event));
        this.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
