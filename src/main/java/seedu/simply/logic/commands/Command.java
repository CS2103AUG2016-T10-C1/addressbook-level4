package seedu.simply.logic.commands;

import java.util.Stack;

import seedu.simply.commons.core.Config;
import seedu.simply.commons.core.EventsCenter;
import seedu.simply.commons.core.Messages;
import seedu.simply.commons.events.ui.IncorrectCommandAttemptedEvent;
import seedu.simply.model.Model;
import seedu.simply.model.SaveState;
import seedu.simply.model.TaskBook;

/**
 * Represents a command with hidden internal logic and the ability to be executed.
 */
public abstract class Command {
    protected Model model;
    
    /**
     * Constructs a feedback message to summarise an operation that displayed a listing of tasks.
     *
     * @param displaySize used to generate summary
     * @param todoSize 
     * @param deadlineSize 
     * @return summary message for tasks displayed
     */
    public static String getMessageForTaskListShownSummary(int displaySize, int deadlineSize, int todoSize) {
            return String.format(Messages.MESSAGE_TASKS_LISTED_OVERVIEW, displaySize, deadlineSize, todoSize);
    }

    /**
     * Executes the command and returns the result message.
     *
     * @return feedback message of the operation result for display
     */
    public abstract CommandResult execute();

    /**
     * Provides any needed dependencies to the command.
     * Commands making use of any of these should override this method to gain
     * access to the dependencies.
     */
    public void setData(Model model) {
        this.model = model;
    }

    /**
     * Raises an event to indicate an attempt to execute an incorrect command
     */
    protected void indicateAttemptToExecuteIncorrectCommand() {
        EventsCenter.getInstance().post(new IncorrectCommandAttemptedEvent(this));
    }
    
}
