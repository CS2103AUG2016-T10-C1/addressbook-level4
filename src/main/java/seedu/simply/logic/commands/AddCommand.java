package seedu.simply.logic.commands;

import seedu.simply.commons.core.EventsCenter;
import seedu.simply.commons.events.ui.JumpToListRequestEvent;
import seedu.simply.commons.exceptions.IllegalValueException;
import seedu.simply.model.tag.Tag;
import seedu.simply.model.tag.UniqueTagList;
import seedu.simply.model.task.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * @@author A0138993L
 * Adds a task to the task book.
 */
public class AddCommand extends Command {

    public static final String COMMAND_WORD = "add";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Adds a to-do task to Simply. "
            + "Parameters: Task details #tag #...."
            + "                                                "
            + " Example: " + COMMAND_WORD
            + " go swimming #IMPT\n" + COMMAND_WORD + ": Adds a deadline task to Simply. "
            + "Parameters: Task details; date; end time #tag #...."
            + "                   "
            + "Example: " + COMMAND_WORD
            + " report; 120516; 1200 #LIFE\n" + COMMAND_WORD + ": Adds a event task to Simply. "
            + "Parameters: [Task details; date; start time; end time] #tag #..."
            + "      "
            + "Example: " + COMMAND_WORD
            + " [siloso beach party; 120716; 1600; 2200] #YOLO #party";

    public static final String EVENT_SUCCESS = "New event added: %1$s";
    public static final String DEADLINE_SUCCESS = "New deadline added: %1$s";
    public static final String TODO_SUCCESS = "New todo added: %1$s";
    public static final String MESSAGE_DUPLICATE_TASK = "This task already exists in Simply";
    public static final String END_TIME_BEFORE_START_TIME_MESSAGE = "The end time cannot be earlier or equal to the start time!";
    public static final String START_TIME_BEFORE_END_TIME_MESSAGE = "The start time cannot be later or equal to the end time!";
    
    private final Task toAdd;
    private static int overdue=0;

    /**
     * Convenience constructor using raw values.
     * @@author A0138993L
     * @throws IllegalValueException if any of the raw values are invalid
     */
    public AddCommand(String name, String date, String start, String end, Set<String> tags)
            throws IllegalValueException {
        final Set<Tag> tagSet = new HashSet<>();
        for (String tagName : tags) {
            tagSet.add(new Tag(tagName));
        }
        this.toAdd = new Task(
                new Name(name),
                new Date(date),
                new Start(start),
                new End(end),
                1,
                0,
                false,
                new UniqueTagList(tagSet)
        );
        if (!startBeforeEnd(toAdd.getStart().toString(), toAdd.getEnd().toString())) {
        	throw new IllegalValueException(END_TIME_BEFORE_START_TIME_MESSAGE);
        }
        if (this.toAdd.getOverdue()==1) {
        	overdue =1;
        }
    }   
    //@@author A0138993L
    public AddCommand(String name, String date, String end, Set<String> tags) //deadline
            throws IllegalValueException {
        final Set<Tag> tagSet = new HashSet<>();
        for (String tagName : tags) {
            tagSet.add(new Tag(tagName));
        }
        this.toAdd = new Task(
                new Name(name),
                new Date(date),
                new Start("no start"),
                new End(end),
                2,
                0,
                false,
                new UniqueTagList(tagSet)
        );
        if (this.toAdd.getOverdue()==1) {
        	overdue =1;
        }
    }
    //@@author A0138993L
    public AddCommand(String name, Set<String> tags) //todos
            throws IllegalValueException {
        final Set<Tag> tagSet = new HashSet<>();
        for (String tagName : tags) {
            tagSet.add(new Tag(tagName));
        }
        this.toAdd = new Task(
                new Name(name),
                new Date("no date"),
                new Start("no start"),
                new End("no end"),
                3,
                0,
                false,
                new UniqueTagList(tagSet)
        );
    }
    
    public static int getOverdue() {
    	return overdue;
    }
    //@@author A0138993L
    private boolean startBeforeEnd(String start, String end) {
		LocalTime start_time = LocalTime.of(Integer.parseInt(start.substring(0,2)), Integer.parseInt(start.substring(2, 4)));
		LocalTime end_time = LocalTime.of(Integer.parseInt(end.substring(0,2)), Integer.parseInt(end.substring(2, 4)));
		if (start_time.isBefore(end_time)) {
			return true;
		} else {
			return false;
		}
	}

    @Override
    public CommandResult execute() {
        assert model != null;
        model.addToUndoStack();
        
        dumpRedoStack();
        model.getCommandHistory().add("add");
        
        try {
            model.addTask(toAdd);
            return addAndSelectTaskToCorrectLIst(toAdd);

        } catch (UniqueTaskList.DuplicateTaskException e) {
            return new CommandResult(MESSAGE_DUPLICATE_TASK);
        }
    }
    /**
     * @@author A0138993L
     * Adds and selects the Task when added into Simply
     * @param toAdd the tasks added
     * @return the command Result whether it is a event, deadline or todo
     */
    private CommandResult addAndSelectTaskToCorrectLIst(Task toAdd) {
        if (toAdd.getTaskCategory() == 1){
            selectEvent(toAdd);
            return new CommandResult(String.format(EVENT_SUCCESS, toAdd));
        } else if (toAdd.getTaskCategory() == 2){
        	selectDeadline(toAdd);
            return new CommandResult(String.format(DEADLINE_SUCCESS, toAdd));
        } else{
            selectTodo(toAdd);
            return new CommandResult(String.format(TODO_SUCCESS, toAdd));
        }
    }
    /**
     * @@author A0138993L
     * selects the added task in the todolist
     * @param toAdd the added task
     */
    private void selectTodo(Task toAdd) {
        char category = 'T';
        int index = model.getFilteredTodoList().indexOf(toAdd);
        EventsCenter.getInstance().post(new JumpToListRequestEvent(index, category));
    }
    /**
     * @@author A0138993L
     * selects the added task in the deadline list
     * @param toAdd  the added task
     */
    private void selectDeadline(Task toAdd) {
        char category = 'D';
        int index = model.getFilteredDeadlineList().indexOf(toAdd);
        EventsCenter.getInstance().post(new JumpToListRequestEvent(index, category));
    }
    /**
     * @@author A0138993L
     * selects the added task in the events list
     * @param toAdd the added task
     */
    private void selectEvent(Task toAdd) {
        char category = 'E';
        int index = model.getFilteredEventList().indexOf(toAdd);
        EventsCenter.getInstance().post(new JumpToListRequestEvent(index, category));
    }
    
    //@@author A0147890U
    // checks if redo stack needs to be dumped and dumps if needed.
    private void dumpRedoStack() {
        ArrayList<String> history = model.getCommandHistory();
        if ((history.size() != 0) && (history.get(history.size() - 1)).equals("undo")) {
            model.getRedoStack().clear();
        }
    }

}
