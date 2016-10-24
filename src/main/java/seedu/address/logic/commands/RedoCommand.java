package seedu.address.logic.commands;

import java.io.IOException;

import seedu.address.commons.core.Config;
import seedu.address.commons.util.ConfigUtil;
import seedu.address.model.SaveState;
import seedu.address.model.TaskBook;

public class RedoCommand extends Command {
    public static final String COMMAND_WORD = "redo";
    
    public static final String MESSAGE_REDO_TASK_SUCCESS = "Redid Task";
    
    private int numTimes;
    
    public RedoCommand() {};
    
    public RedoCommand(int numTimes) {
        this.numTimes = numTimes;
        
    }
    
    @Override
    public CommandResult execute() {
        assert model != null;
        for (int i = 0; i < numTimes; i++) {
            TaskBook currentTaskBook = new TaskBook(model.getAddressBook());
            
            
            SaveState saveToResetTo = model.getRedoStack().pop();
            TaskBook taskToResetTo = saveToResetTo.getSaveStateTaskBook();
            model.resetData(taskToResetTo);
            
            Config currentConfig = new Config(model.getConfig());
            Config config = saveToResetTo.getSaveStateConfig();
            model.setConfig(config);
            
            System.out.println(config.getAddressBookFilePath());
            try {
                ConfigUtil.saveConfig(config, Config.DEFAULT_CONFIG_FILE);
                System.out.println("Pretty please");
            } catch (IOException e) {
                System.out.println("omg wtf am i doing");
            }
           
            SaveState saveToBeAdded = new SaveState(currentTaskBook, currentConfig);
            model.getUndoStack().push(saveToBeAdded);
        }
        return new CommandResult(MESSAGE_REDO_TASK_SUCCESS);
    }
    
}