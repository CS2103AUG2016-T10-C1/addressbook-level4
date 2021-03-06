package seedu.simply.model;

import java.util.ArrayList;
import java.util.Set;
import java.util.Stack;

import seedu.simply.commons.core.Config;
import seedu.simply.commons.core.UnmodifiableObservableList;
import seedu.simply.commons.exceptions.IllegalValueException;
import seedu.simply.logic.commands.Command;
import seedu.simply.model.task.ReadOnlyTask;
import seedu.simply.model.task.Task;
import seedu.simply.model.task.UniqueTaskList;
import seedu.simply.model.task.UniqueTaskList.TaskNotFoundException;

/**
 * The API of the Model component.
 */
public interface Model {
    /** Clears existing backing model and replaces with the provided new data. */
    void resetData(ReadOnlyTaskBook newData);

    /** Returns the TaskBook */
    ReadOnlyTaskBook getTaskBook();

    /** Deletes the given task. */
    void deleteTask(ReadOnlyTask target);

    /** Adds the given task */
    void addTask(Task task) throws UniqueTaskList.DuplicateTaskException;
    
    /** Checks for overdue task */
    void overdueTask();

    /** Returns the filtered task list as an {@code UnmodifiableObservableList<ReadOnlyTask>} */
    UnmodifiableObservableList<ReadOnlyTask> getFilteredEventList();
    UnmodifiableObservableList<ReadOnlyTask> getFilteredDeadlineList();
    UnmodifiableObservableList<ReadOnlyTask> getFilteredTodoList();
    
    /** Updates the filter of the filtered task list to show all tasks */
    void updateFilteredListToShowAll();
    
    /** Updates the filter of the filtered task list to show all completed tasks */
    void updateFilteredListToShowAllCompleted();
    
    /** Updates the filter of the filtered task list to show all uncompleted tasks */
    void updateFilteredListToShowAllUncompleted();

    /** Updates the filter of the filtered task list to filter by the given keywords*/
    void updateFilteredEventList(Set<String> keywords);
    void updateFilteredDeadlineList(Set<String> keywords);
    void updateFilteredTodoList(Set<String> keywords);

    /** Edits the given task 
     * @throws IllegalValueException 
     * @throws TaskNotFoundException */
    Task editTask(ReadOnlyTask target, String args, char category) throws TaskNotFoundException, IllegalValueException;
    
    /**
     * Mark the given task as done
     */
    void markDone(ReadOnlyTask target) throws TaskNotFoundException;

     /* Returns the undo stack
     * @return undo stack
     */
    Stack<SaveState> getUndoStack();
    
    /*
     * Returns the redo stack
     */
    Stack<SaveState> getRedoStack();
    
    /**
     * Returns the command history arraylist.
     */
    ArrayList<String> getCommandHistory();
    
    /**
     * Add to undo stack
     */
    void addToUndoStack();
    
    /**
     * Returns the config object 
     */
    Config getConfig();
    
    /**
     * Sets the config object
     */
    void setConfig(Config config);
    /**
     * finds all tasks in the wrong category and changes it to the correct category
     */
    void changeTaskCategory();
    /**
     * Check if the given Task exist in the Simply
     */
    boolean checkTask(ReadOnlyTask target);
}
