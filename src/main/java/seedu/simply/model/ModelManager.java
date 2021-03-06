package seedu.simply.model;

import java.util.ArrayList;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Logger;

import javafx.collections.transformation.FilteredList;
import seedu.simply.commons.core.ComponentManager;
import seedu.simply.commons.core.Config;
import seedu.simply.commons.core.LogsCenter;
import seedu.simply.commons.core.UnmodifiableObservableList;
import seedu.simply.commons.events.model.TaskBookChangedEvent;
import seedu.simply.commons.events.ui.OverdueChangedEvent;
import seedu.simply.commons.exceptions.IllegalValueException;
import seedu.simply.commons.util.StringUtil;
import seedu.simply.logic.commands.Command;
import seedu.simply.model.task.ReadOnlyTask;
import seedu.simply.model.task.Task;
import seedu.simply.model.task.UniqueTaskList;
import seedu.simply.model.task.UniqueTaskList.DuplicateTaskException;
import seedu.simply.model.task.UniqueTaskList.TaskNotFoundException;

import java.util.Set;
import java.util.logging.Logger;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Represents the in-memory model of the task book data.
 * All changes to any model should be synchronized.
 */
public class ModelManager extends ComponentManager implements Model {
    private static final Logger logger = LogsCenter.getLogger(ModelManager.class);
    final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final TaskBook taskBook;
    private final FilteredList<Task> filteredEvents;
    private final Stack<SaveState> undoStack;
    private final Stack<SaveState> redoStack;
    private final ArrayList<String> commandHistory;
    private Config config;
    private FilteredList<Task> filteredDeadlines;
    private FilteredList<Task> filteredTodos;

    /**
     * Initializes a ModelManager with the given TaskBook
     * TaskBook and its variables should not be null
     */
    public ModelManager(TaskBook src, UserPrefs userPrefs, Config config) {
        super();
        assert src != null;
        assert userPrefs != null;

        logger.fine("Initializing with task book: " + src + " and user prefs " + userPrefs);

        taskBook = new TaskBook(src);
        filteredEvents = new FilteredList<>(taskBook.getEvents());
        filteredDeadlines = new FilteredList<>(taskBook.getDeadlines());
        filteredTodos = new FilteredList<>(taskBook.getTodo());
        undoStack = new Stack<SaveState>();
        redoStack = new Stack<SaveState>();
        commandHistory = new ArrayList<String>();
        this.config = config;
    }

    public ModelManager() {
        this(new TaskBook(), new UserPrefs(), new Config());
    }

    public ModelManager(ReadOnlyTaskBook initialData, UserPrefs userPrefs, Config config) {
        taskBook = new TaskBook(initialData);
        filteredEvents = new FilteredList<>(taskBook.getEvents());
        filteredDeadlines = new FilteredList<>(taskBook.getDeadlines());
        filteredTodos = new FilteredList<>(taskBook.getTodo());
        undoStack = new Stack<SaveState>();
        redoStack = new Stack<SaveState>();
        commandHistory = new ArrayList<String>();
        this.config = config;
    }

    //@@author A0147890U
    @Override
    public ArrayList<String> getCommandHistory() {
        return commandHistory;
    }

    //@@author A0147890U
    @Override
    public void addToUndoStack() {
        TaskBook taskBookToBeAdded = new TaskBook(taskBook);
        //Config configToBeAdded = new Config(config);
        SaveState saveToBeAdded = new SaveState(taskBookToBeAdded, config);

        undoStack.push(saveToBeAdded);
    }

    //@@author A0147890U
    @Override
    public Config getConfig() {
        return config;
    }

    //@@author A0147890U
    @Override
    public void setConfig(Config config) {
        this.config = config;
    }

    //@@author A0147890U
    @Override 
    public Stack<SaveState> getUndoStack() {
        return this.undoStack;
    }

    //@@author A0147890U
    @Override
    public Stack<SaveState> getRedoStack() {
        return this.redoStack;
    }

    //@@author
    @Override
    public void resetData(ReadOnlyTaskBook newData) {
        taskBook.resetData(newData);
        updateFilteredListToShowAllUncompleted();
        indicateTaskBookChanged();
    }

    @Override
    public ReadOnlyTaskBook getTaskBook() {
        return taskBook;
    }

    /** Raises an event to indicate the model has changed */
    private void indicateTaskBookChanged() {
        raise(new TaskBookChangedEvent(taskBook));
    }

    //@@author A0147890U
    /** Raises an event to indicate task overdue status might have changed */
    private void indicateTaskOverdueChanged() {
        raise(new OverdueChangedEvent());
    }

    //@@author A0139430L
    @Override
    public synchronized void deleteTask(ReadOnlyTask target) {
        taskBook.removeTask(target);
        indicateTaskBookChanged();
    }
    
    //@@author A0139430L
    @Override
    public synchronized boolean checkTask(ReadOnlyTask target) {
        return taskBook.checkTask(target);
    }

    @Override 
    public synchronized Task editTask(ReadOnlyTask target, String args, char category) throws TaskNotFoundException, IllegalValueException {
        Task temp = taskBook.changeTask(target, args, category);
        updateFilteredListToShowAllUncompleted();
        indicateTaskBookChanged();
        return temp;
    }

    @Override
    public synchronized void addTask(Task task) throws UniqueTaskList.DuplicateTaskException {
        taskBook.addTask(task);
        updateFilteredListToShowAllUncompleted();
        indicateTaskBookChanged();
    }

    //@@author A0135722L Zhiyuan
    public synchronized void markDone(ReadOnlyTask target) throws TaskNotFoundException {
        taskBook.completeTask(target);
        updateFilteredListToShowAllUncompleted();
        indicateTaskBookChanged();
    }

    //@@author A0138993L
    @Override
    public synchronized void overdueTask() {
        final Runnable overdue = new Runnable() {
            public void run() {
                taskBook.overdueTask();
                indicateTaskOverdueChanged();
                indicateTaskBookChanged();
            };
        };
        scheduler.scheduleAtFixedRate(overdue, 0, 1, TimeUnit.SECONDS); 
    }

    //@@author A0139430L
    @Override
    public synchronized void changeTaskCategory() {
        try {
            taskBook.changeTaskCategory();
        } catch (DuplicateTaskException | TaskNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        indicateTaskBookChanged();
    }

    //=========== Filtered Task List Accessors ===============================================================

    @Override
    public UnmodifiableObservableList<ReadOnlyTask> getFilteredEventList() {
        return new UnmodifiableObservableList<>(filteredEvents);
    }

    public UnmodifiableObservableList<ReadOnlyTask> getFilteredDeadlineList() {
        return new UnmodifiableObservableList<>(filteredDeadlines);
    }

    public UnmodifiableObservableList<ReadOnlyTask> getFilteredTodoList() {
        return new UnmodifiableObservableList<>(filteredTodos);
    }

    @Override
    public void updateFilteredListToShowAll() {
        filteredEvents.setPredicate(null);
        filteredDeadlines.setPredicate(null);
        filteredTodos.setPredicate(null);
    }

    //@@author A0147890U
    @Override
    public void updateFilteredListToShowAllCompleted() {
        filteredEvents.setPredicate(task -> {
            if (task.getIsCompleted()) {
                return true;
            }
            return false;
        });
        filteredDeadlines.setPredicate(task -> {
            if (task.getIsCompleted()) {
                return true;
            }
            return false;
        });
        filteredTodos.setPredicate(task -> {
            if (task.getIsCompleted()) {
                return true;
            }
            return false;
        });
    }

    //@@author A0147890U
    @Override 
    public void updateFilteredListToShowAllUncompleted() {
        filteredEvents.setPredicate(task -> {
            if (!task.getIsCompleted()) {
                return true;
            }
            return false;
        });
        filteredDeadlines.setPredicate(task -> {
            if (!task.getIsCompleted()) {
                return true;
            }
            return false;
        });
        filteredTodos.setPredicate(task -> {
            if (!task.getIsCompleted()) {
                return true;
            }
            return false;
        });

    }

    //@@author
    @Override
    public void updateFilteredEventList(Set<String> keywords){
        updateFilteredEventList(new PredicateExpression(new NameQualifier(keywords)));
    }

    @Override
    public void updateFilteredDeadlineList(Set<String> keywords){
        updateFilteredDeadlineList(new PredicateExpression(new NameQualifier(keywords)));
    }

    @Override
    public void updateFilteredTodoList(Set<String> keywords){
        updateFilteredTodoList(new PredicateExpression(new NameQualifier(keywords)));
    }

    private void updateFilteredEventList(Expression expression) {
        filteredEvents.setPredicate(expression::satisfies);
    }

    private void updateFilteredDeadlineList(Expression expression) {
        filteredDeadlines.setPredicate(expression::satisfies);
    }

    private void updateFilteredTodoList(Expression expression) {
        filteredTodos.setPredicate(expression::satisfies);
    }

    //========== Inner classes/interfaces used for filtering ==================================================

    interface Expression {
        boolean satisfies(ReadOnlyTask task);
        String toString();
    }

    private class PredicateExpression implements Expression {

        private final Qualifier qualifier;

        PredicateExpression(Qualifier qualifier) {
            this.qualifier = qualifier;
        }

        @Override
        public boolean satisfies(ReadOnlyTask task) {
            return qualifier.run(task);
        }

        @Override
        public String toString() {
            return qualifier.toString();
        }
    }

    interface Qualifier {
        boolean run(ReadOnlyTask task);
        String toString();
    }

    private class NameQualifier implements Qualifier {
        private Set<String> anyKeyWords;

        NameQualifier(Set<String> anyKeyWords) {
            this.anyKeyWords = anyKeyWords;
        }
        //@@author A0139430L
        @Override
        public boolean run(ReadOnlyTask Task) {

            return anyKeyWords.stream()
                    .filter(keyword -> (StringUtil.containsIgnoreCase(Task.getName().taskDetails.toLowerCase(), keyword)
                            || StringUtil.containsIgnoreCase(Task.getDate().value, keyword)
                            || StringUtil.containsIgnoreCase(Task.getStart().value, keyword)
                            || StringUtil.containsIgnoreCase(Task.getEnd().value, keyword)
                            || StringUtil.containsIgnoreCase(Task.getTags().toString(), keyword))
                            && Task.getIsCompleted() == false)
                    .findAny()
                    .isPresent();
        }

        @Override
        public String toString() {
            return "name=" + String.join(", ", anyKeyWords);
        }
    }


}
