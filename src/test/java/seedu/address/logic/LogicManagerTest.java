package seedu.address.logic;

import com.google.common.eventbus.Subscribe;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import seedu.address.commons.core.EventsCenter;
import seedu.address.commons.core.Messages;
import seedu.address.commons.core.UnmodifiableObservableList;
import seedu.address.logic.commands.*;
import seedu.address.commons.events.ui.JumpToListRequestEvent;
import seedu.address.commons.events.ui.ShowHelpRequestEvent;
import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.commons.events.model.AddressBookChangedEvent;
import seedu.address.model.TaskBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyTaskBook;
import seedu.address.model.tag.Tag;
import seedu.address.model.tag.UniqueTagList;
import seedu.address.model.task.*;
import seedu.address.model.task.UniqueTaskList.TaskNotFoundException;
import seedu.address.storage.StorageManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static seedu.address.commons.core.Messages.*;

public class LogicManagerTest {

    /**
     * See https://github.com/junit-team/junit4/wiki/rules#temporaryfolder-rule
     */
    @Rule
    public TemporaryFolder saveFolder = new TemporaryFolder();

    private Model model;
    private Logic logic;

    //These are for checking the correctness of the events raised
    private ReadOnlyTaskBook latestSavedAddressBook;
    private boolean helpShown;
    private int targetedJumpIndex;

    @Subscribe
    private void handleLocalModelChangedEvent(AddressBookChangedEvent abce) {
        latestSavedAddressBook = new TaskBook(abce.data);
    }

    @Subscribe
    private void handleShowHelpRequestEvent(ShowHelpRequestEvent she) {
        helpShown = true;
    }

    @Subscribe
    private void handleJumpToListRequestEvent(JumpToListRequestEvent je) {
        targetedJumpIndex = je.targetIndex;
    }

    @Before
    public void setup() {
        model = new ModelManager();
        String tempAddressBookFile = saveFolder.getRoot().getPath() + "TempAddressBook.xml";
        String tempPreferencesFile = saveFolder.getRoot().getPath() + "TempPreferences.json";
        logic = new LogicManager(model);
        EventsCenter.getInstance().registerHandler(this);

        latestSavedAddressBook = new TaskBook(model.getAddressBook()); // last saved assumed to be up to date before.
        helpShown = false;
        targetedJumpIndex = -1; // non yet
    }

    @After
    public void teardown() {
        EventsCenter.clearSubscribers();
    }

    @Test
    public void execute_invalid() throws Exception {
        String invalidCommand = "       ";
        assertCommandBehavior(invalidCommand,
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, HelpCommand.MESSAGE_USAGE));
    }

    /**
     * Executes the command and confirms that the result message is correct.
     * Both the 'address book' and the 'last shown list' are expected to be empty.
     * @see #assertCommandBehavior(String, String, ReadOnlyTaskBook, List)
     */
    private void assertCommandBehavior(String inputCommand, String expectedMessage) throws Exception {
        assertCommandBehavior(inputCommand, expectedMessage, new TaskBook(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Executes the command and confirms that the result message is correct and
     * also confirms that the following three parts of the LogicManager object's state are as expected:<br>
     *      - the internal address book data are same as those in the {@code expectedAddressBook} <br>
     *      - the backing list shown by UI matches the {@code shownList} <br>
     *      - {@code expectedAddressBook} was saved to the storage file. <br>
     */
    //@@author A0138993L
    private void assertCommandBehavior(String inputCommand, String expectedMessage,
            ReadOnlyTaskBook expectedAddressBook,
            List<? extends ReadOnlyTask> expectedShownList,
            List<? extends ReadOnlyTask> expectedDeadlineShownList,
            List<? extends ReadOnlyTask> expectedTodoShownList) throws Exception {

        //Execute the command
        CommandResult result = logic.execute(inputCommand);

        //Confirm the ui display elements should contain the right data
        assertEquals(expectedMessage, result.feedbackToUser);
        assertEquals(expectedShownList, model.getFilteredEventList());
        assertEquals(expectedDeadlineShownList, model.getFilteredDeadlineList());
        assertEquals(expectedTodoShownList, model.getFilteredTodoList());

        //Confirm the state of data (saved and in-memory) is as expected
        assertEquals(expectedAddressBook, model.getAddressBook());
        assertEquals(expectedAddressBook, latestSavedAddressBook);
    }


    @Test
    public void execute_unknownCommandWord() throws Exception {
        String unknownCommand = "uicfhmowqewca";
        assertCommandBehavior(unknownCommand, MESSAGE_UNKNOWN_COMMAND);
    }

    @Test
    public void execute_help() throws Exception {
        assertCommandBehavior("help", HelpCommand.SHOWING_HELP_MESSAGE);
        assertTrue(helpShown);
    }

    @Test
    public void execute_exit() throws Exception {
        assertCommandBehavior("exit", ExitCommand.MESSAGE_EXIT_ACKNOWLEDGEMENT);
    }

    @Test
    public void execute_clear() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        List<Task> threePersons = helper.generateEventsList(3);
        List<Task> threeDeadlines = helper.generateDeadlineList(3);
        List<Task> threeTodos = helper.generateTodoList(3);

        TaskBook expectedAB = helper.generateAddressBook(threePersons, threeDeadlines, threeTodos);
        expectedAB.resetData(TaskBook.getEmptyAddressBook());
        assertCommandBehavior("clear", ClearCommand.MESSAGE_SUCCESS, expectedAB, 
                expectedAB.getEventList(), expectedAB.getDeadlineList(), expectedAB.getTodoList());
    }


    @Test
    public void execute_add_invalidArgsFormat() throws Exception {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, AddCommand.MESSAGE_USAGE);
        assertCommandBehavior(
                "add", expectedMessage);
    }


    @Test
    public void execute_add_invalidPersonData() throws Exception {
        assertCommandBehavior(
                "add [events;]", Name.MESSAGE_NAME_CONSTRAINTS);
        assertCommandBehavior(
                "add deadline; 123123", Date.MESSAGE_DATE_CONSTRAINTS);
        assertCommandBehavior(
                "add [events; 121221; 5555]", Start.MESSAGE_START_CONSTRAINTS);
        assertCommandBehavior(
                "add deadlines; 121212; 12.30am #..", Tag.MESSAGE_TAG_CONSTRAINTS);

    }
    
    //@@author A0139430L JingRui
    @Test
    public void execute_add_Event_successful() throws Exception {
        // setup expectations
        TestDataHelper helper = new TestDataHelper();
        Task toBeAdded = helper.adam();
        TaskBook expectedAB = new TaskBook();
        expectedAB.addTask(toBeAdded);

        // execute command and verify result
        assertCommandBehavior(helper.generateAddCommand(toBeAdded),
                String.format(AddCommand.EVENT_SUCCESS, toBeAdded),
                expectedAB,
                expectedAB.getEventList(),
                expectedAB.getDeadlineList(),
                expectedAB.getTodoList());

    }
    //@@author A0139430L JingRui
    @Test
    public void execute_add_Deadline_successful() throws Exception {
        // setup expectations
        TestDataHelper helper = new TestDataHelper();
        Task toBeAdded = helper.beta();
        TaskBook expectedAB = new TaskBook();
        expectedAB.addTask(toBeAdded);

        assertCommandBehavior(helper.generateAddDeadlineCommand(toBeAdded),
                String.format(AddCommand.DEADLINE_SUCCESS, toBeAdded),
                expectedAB,
                expectedAB.getEventList(),
                expectedAB.getDeadlineList(),
                expectedAB.getTodoList());

    }
    //@@author A0139430L JingRui
    @Test
    public void execute_add_Todo_successful() throws Exception {
        // setup expectations
        TestDataHelper helper = new TestDataHelper();
        Task toBeAdded = helper.charlie();
        TaskBook expectedAB = helper.generateAddressBook(1, 1, 1);
        TaskBook undolist = new TaskBook(expectedAB);
        helper.addToModel(model, 1, 1, 1);
        expectedAB.addTask(toBeAdded);

        assertCommandBehavior(helper.generateAddTodoCommand(toBeAdded),
                String.format(AddCommand.TODO_SUCCESS, toBeAdded),
                expectedAB,
                expectedAB.getEventList(),
                expectedAB.getDeadlineList(),
                expectedAB.getTodoList());
        
        assertCommandBehavior("undo 1", UndoCommand.MESSAGE_UNDO_TASK_SUCCESS,
                undolist,
                undolist.getEventList(),
                undolist.getDeadlineList(),
                undolist.getTodoList());

    }
    

    //@@author A0139430L JingRui
    @Test
    public void execute_addDuplicateEvent_notAllowed() throws Exception {
        // setup expectations
        TestDataHelper helper = new TestDataHelper();
        Task toBeAdded = helper.adam();
        TaskBook expectedAB = new TaskBook();
        expectedAB.addTask(toBeAdded);

        // setup starting state
        model.addTask(toBeAdded); // person already in internal address book

        // execute command and verify result
        assertCommandBehavior(
                helper.generateAddCommand(toBeAdded),
                AddCommand.MESSAGE_DUPLICATE_TASK,
                expectedAB,
                expectedAB.getEventList(),
                Collections.emptyList(),
                Collections.emptyList());
    }
    //@@author A0139430L JingRui
    @Test
    public void execute_addDuplicateDeadline_notAllowed() throws Exception {
        // setup expectations
        TestDataHelper helper = new TestDataHelper();
        Task toBeAdded = helper.beta();
        TaskBook expectedAB = new TaskBook();
        expectedAB.addTask(toBeAdded);

        // setup starting state
        model.addTask(toBeAdded); // person already in internal address book

        // execute command and verify result
        assertCommandBehavior(
                helper.generateAddDeadlineCommand(toBeAdded),
                AddCommand.MESSAGE_DUPLICATE_TASK,
                expectedAB,
                Collections.emptyList(),
                expectedAB.getDeadlineList(),
                Collections.emptyList());
    }
    //@@author A0139430L JingRui
    @Test
    public void execute_addDuplicateTodo_notAllowed() throws Exception {
        // setup expectations
        TestDataHelper helper = new TestDataHelper();
        Task toBeAdded = helper.charlie();
        TaskBook expectedAB = new TaskBook();
        expectedAB.addTask(toBeAdded);

        // setup starting state
        model.addTask(toBeAdded); // person already in internal address book

        // execute command and verify result
        assertCommandBehavior(
                helper.generateAddTodoCommand(toBeAdded),
                AddCommand.MESSAGE_DUPLICATE_TASK,
                expectedAB,
                Collections.emptyList(),
                Collections.emptyList(),
                expectedAB.getTodoList());
    }
    
    @Test
    public void execute_invalidListFormat() throws Exception {
        assertCommandBehavior("list asdasd",
                String.format(MESSAGE_INVALID_COMMAND_FORMAT, ListCommand.MESSAGE_USAGE));
    }

    //@@author A0139430L JingRui
    @Test
    public void execute_list_showsAllPersons() throws Exception {
        // prepare expectations
        TestDataHelper helper = new TestDataHelper();
        TaskBook expectedAB = helper.generateAddressBook(2, 2, 2);
        List<? extends ReadOnlyTask> expectedList = expectedAB.getEventList();
        List<? extends ReadOnlyTask> expectedDeadlineList = expectedAB.getDeadlineList();
        List<? extends ReadOnlyTask> expectedTodoList = expectedAB.getTodoList();
        // prepare address book state
        helper.addToModel(model, 2, 2, 2);

        assertCommandBehavior("list",
                ListCommand.MESSAGE_SUCCESS,
                expectedAB,
                expectedList,
                expectedDeadlineList,
                expectedTodoList);
    }
    //@@author A0138993L
    @Test
    public void execute_list_done_events() throws Exception {
        // prepare expectations
        TestDataHelper helper = new TestDataHelper();
        Task done = helper.adam();
        Task undone1 = helper.beta();
        Task undone2 = helper.charlie();
        List<Task> eventList = helper.generateEventList(done);
        List<Task> deadlineList = helper.generateDeadlineList(undone1);
        List<Task> todoList = helper.generateTodoList(undone2);
        TaskBook expectedAB = helper.generateAddressBook(eventList, deadlineList, todoList);
        expectedAB.completeTask(done);
        helper.addToModel(model, eventList, deadlineList, todoList);
        model.markDone(done);

        assertCommandBehavior("list done",
                ListCommand.MESSAGE_SUCCESS,
                expectedAB,
                eventList,
                Collections.emptyList(),
                Collections.emptyList());
    }
    //@@author A0138993L
    @Test
    public void execute_list_done_deadline() throws Exception {
        // prepare expectations
        TestDataHelper helper = new TestDataHelper();
        Task undone1 = helper.adam();
        Task done = helper.beta();
        Task undone2 = helper.charlie();
        List<Task> eventList = helper.generateEventList(undone1);
        List<Task> deadlineList = helper.generateDeadlineList(done);
        List<Task> todoList = helper.generateTodoList(undone2);
        TaskBook expectedAB = helper.generateAddressBook(eventList, deadlineList, todoList);
        expectedAB.completeTask(done);
        helper.addToModel(model, eventList, deadlineList, todoList);
        model.markDone(done);

        assertCommandBehavior("list done",
                ListCommand.MESSAGE_SUCCESS,
                expectedAB,
                Collections.emptyList(),
                deadlineList,
                Collections.emptyList());
    }
    //@@author A0138993L
    @Test
    public void execute_list_done_todo() throws Exception {
        // prepare expectations
        TestDataHelper helper = new TestDataHelper();
        Task undone1 = helper.adam();
        Task undone2 = helper.beta();
        Task done = helper.charlie();
        List<Task> eventList = helper.generateEventList(undone1);
        List<Task> deadlineList = helper.generateDeadlineList(undone2);
        List<Task> todoList = helper.generateTodoList(done);
        TaskBook expectedAB = helper.generateAddressBook(eventList, deadlineList, todoList);
        expectedAB.completeTask(done);
        helper.addToModel(model, eventList, deadlineList, todoList);
        model.markDone(done);

        assertCommandBehavior("list done",
                ListCommand.MESSAGE_SUCCESS,
                expectedAB,
                Collections.emptyList(),
                Collections.emptyList(),
                todoList);
    }
    /*
    @Test
    public void execute_one_undo() throws Exception {
    	TestDataHelper helper = new TestDataHelper();
    	Task toBeAdded = helper.adam();
    	Task toBeAdded1 = helper.beta();
    	Task toBeAdded2 = helper.charlie();
    	List<Task> eventList = helper.generateEventList(toBeAdded);
    	List<Task> deadlineList = helper.generateEventList(toBeAdded1);
    	List<Task> todoList = helper.generateEventList(toBeAdded2);
    	TaskBook expectedAB = helper.generateAddressBook(eventList, deadlineList, todoList);
    	helper.addToModel(model, eventList, deadlineList, todoList);
    	model.addToUndoStack();
    	expectedAB.addTask(helper.generateDeadline(1));


    	assertCommandBehavior("undo 1", UndoCommand.MESSAGE_UNDO_TASK_SUCCESS,
    			expectedAB,
    			expectedAB.getEventList(),
    			expectedAB.getDeadlineList(),
    			expectedAB.getTodoList());
    }
     */
    //@@author A0138993L
    @Test
    public void execute_done_markCorrectEvent() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        Task toBeMarked = helper.adam();
        List<Task> eventList = helper.generateEventList(toBeMarked);
        TaskBook expectedAB = helper.generateAddressBook(eventList, Collections.emptyList(), Collections.emptyList());
        expectedAB.completeTask(toBeMarked);
        helper.addToModel(model, eventList, Collections.emptyList(),  Collections.emptyList());

        assertCommandBehavior("done E1", 
                String.format(DoneCommand.MESSAGE_MARK_DONE_SUCCESS, new String("[E1]")),
                expectedAB,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList());
    }

    //@@author A0138993L
    @Test
    public void execute_done_markCorrectEventRange() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        Task toBeMarked1 = helper.generateEventWithName("lala");
        Task toBeMarked2 = helper.generateEventWithName("lalala");
        Task toBeMarked3 = helper.generateEventWithName("lalalala");
        List<Task> eventList = helper.generateEventList(toBeMarked1, toBeMarked2, toBeMarked3);
        TaskBook expectedAB = helper.generateAddressBook(eventList, Collections.emptyList(), Collections.emptyList());
        expectedAB.completeTask(toBeMarked1);
        expectedAB.completeTask(toBeMarked2);
        expectedAB.completeTask(toBeMarked3);
        helper.addToModel(model, eventList, Collections.emptyList(),  Collections.emptyList());

        assertCommandBehavior("done E1-E3", 
                String.format(DoneCommand.MESSAGE_MARK_DONE_SUCCESS, new String("[E1, E2, E3]")),
                expectedAB,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList());
    }

    //@@author A0138993L
    @Test
    public void execute_done_markCorrectEventMultiple() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        Task toBeMarked1 = helper.generateEventWithName("lala");
        Task unmarked = helper.generateEventWithName("lalala");
        Task toBeMarked3 = helper.generateEventWithName("lalalala");
        List<Task> eventList = helper.generateEventList(toBeMarked1, unmarked, toBeMarked3);
        List<Task> unmarkedList = helper.generateEventList(unmarked);
        TaskBook expectedAB = helper.generateAddressBook(eventList, Collections.emptyList(), Collections.emptyList());
        expectedAB.completeTask(toBeMarked1);
        expectedAB.completeTask(toBeMarked3);
        helper.addToModel(model, eventList, Collections.emptyList(),  Collections.emptyList());

        assertCommandBehavior("done E1, E3", 
                String.format(DoneCommand.MESSAGE_MARK_DONE_SUCCESS, new String("[E1, E3]")),
                expectedAB,
                unmarkedList,
                Collections.emptyList(),
                Collections.emptyList());
    }

    //@@author A0138993L
    @Test
    public void execute_done_markCorrectDeadline() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        Task toBeMarked = helper.beta();
        List<Task> deadlineList = helper.generateDeadlineList(toBeMarked);
        TaskBook expectedAB = helper.generateAddressBook(Collections.emptyList(), deadlineList, Collections.emptyList());
        expectedAB.completeTask(toBeMarked);
        helper.addToModel(model, Collections.emptyList(), deadlineList, Collections.emptyList());

        assertCommandBehavior("done D1", 
                String.format(DoneCommand.MESSAGE_MARK_DONE_SUCCESS, new String("[D1]")),
                expectedAB,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList());
    }

    //@@author A0138993L
    @Test
    public void execute_done_markCorrectDeadlineRange() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        Task toBeMarked1 = helper.generateDeadlineWithName("la");
        Task toBeMarked2 = helper.generateDeadlineWithName("lala");
        Task toBeMarked3 = helper.generateDeadlineWithName("lalala");
        List<Task> deadlineList = helper.generateDeadlineList(toBeMarked1, toBeMarked2, toBeMarked3);
        TaskBook expectedAB = helper.generateAddressBook(Collections.emptyList(), deadlineList, Collections.emptyList());
        expectedAB.completeTask(toBeMarked1);
        expectedAB.completeTask(toBeMarked2);
        expectedAB.completeTask(toBeMarked3);
        helper.addToModel(model, Collections.emptyList(), deadlineList, Collections.emptyList());

        assertCommandBehavior("done D1-D3", 
                String.format(DoneCommand.MESSAGE_MARK_DONE_SUCCESS, new String("[D1, D2, D3]")),
                expectedAB,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList());
    }

    //@@author A0138993L
    @Test
    public void execute_done_markCorrectDeadlineMultiple() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        Task toBeMarked1 = helper.generateDeadlineWithName("la");
        Task unmarked = helper.generateDeadlineWithName("lala");
        Task toBeMarked3 = helper.generateDeadlineWithName("lalala");
        List<Task> deadlineList = helper.generateDeadlineList(toBeMarked1, unmarked, toBeMarked3);
        List<Task> unmarkedList = helper.generateDeadlineList(unmarked);
        TaskBook expectedAB = helper.generateAddressBook(Collections.emptyList(), deadlineList, Collections.emptyList());
        expectedAB.completeTask(toBeMarked1);
        expectedAB.completeTask(toBeMarked3);
        helper.addToModel(model, Collections.emptyList(), deadlineList, Collections.emptyList());

        assertCommandBehavior("done D1, D3", 
                String.format(DoneCommand.MESSAGE_MARK_DONE_SUCCESS, new String("[D1, D3]")),
                expectedAB,
                Collections.emptyList(),
                unmarkedList,
                Collections.emptyList());
    }

    //@@author A0138993L
    @Test
    public void execute_done_markCorrectTodo() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        Task toBeMarked = helper.charlie();
        List<Task> todoList = helper.generateTodoList(toBeMarked);
        TaskBook expectedAB = helper.generateAddressBook(Collections.emptyList(), Collections.emptyList(), todoList);
        expectedAB.completeTask(toBeMarked);
        helper.addToModel(model, Collections.emptyList(),  Collections.emptyList(), todoList);

        assertCommandBehavior("done T1", 
                String.format(DoneCommand.MESSAGE_MARK_DONE_SUCCESS, new String("[T1]")),
                expectedAB,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList());
    }

    //@@author A0138993L
    @Test
    public void execute_done_markCorrectTodoRange() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        Task toBeMarked1 = helper.generateTodoWithName("la");
        Task toBeMarked2 = helper.generateTodoWithName("lala");
        Task toBeMarked3 = helper.generateTodoWithName("lalala");
        List<Task> todoList = helper.generateTodoList(toBeMarked1, toBeMarked2, toBeMarked3);
        TaskBook expectedAB = helper.generateAddressBook(Collections.emptyList(), Collections.emptyList(), todoList);
        expectedAB.completeTask(toBeMarked1);
        expectedAB.completeTask(toBeMarked2);
        expectedAB.completeTask(toBeMarked3);
        helper.addToModel(model, Collections.emptyList(),  Collections.emptyList(), todoList);

        assertCommandBehavior("done T1-T3", 
                String.format(DoneCommand.MESSAGE_MARK_DONE_SUCCESS, new String("[T1, T2, T3]")),
                expectedAB,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList());
    }

    //@@author A0138993L
    @Test
    public void execute_done_markCorrectTodoMultiple() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        Task toBeMarked1 = helper.generateTodoWithName("la");
        Task unmarked = helper.generateTodoWithName("lala");
        Task toBeMarked3 = helper.generateTodoWithName("lalala");
        List<Task> unmarkedList = helper.generateTodoList(unmarked);
        List<Task> todoList = helper.generateTodoList(toBeMarked1, unmarked, toBeMarked3);
        TaskBook expectedAB = helper.generateAddressBook(Collections.emptyList(), Collections.emptyList(), todoList);
        expectedAB.completeTask(toBeMarked1);
        expectedAB.completeTask(toBeMarked3);
        helper.addToModel(model, Collections.emptyList(),  Collections.emptyList(), todoList);

        assertCommandBehavior("done T1, T3", 
                String.format(DoneCommand.MESSAGE_MARK_DONE_SUCCESS, new String("[T1, T3]")),
                expectedAB,
                Collections.emptyList(),
                Collections.emptyList(),
                unmarkedList);
    }

    //@@author A0138993L
    @Test
    public void execute_done_noEventIndex() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        Task toBeMarked = helper.adam();
        List<Task> eventList = helper.generateEventList(toBeMarked);
        TaskBook expectedAB = helper.generateAddressBook(eventList, Collections.emptyList(), Collections.emptyList());
        expectedAB.completeTask(toBeMarked);
        helper.addToModel(model, eventList, Collections.emptyList(), Collections.emptyList());

        assertCommandBehavior("done E3",
                Messages.MESSAGE_INVALID_TASK_DISPLAYED_INDEX,
                expectedAB,
                eventList,
                Collections.emptyList(),
                Collections.emptyList());
    }

    //@@author A0138993L
    @Test
    public void execute_done_noDeadlineIndex() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        Task toBeMarked = helper.beta();
        List<Task> deadlineList = helper.generateDeadlineList(toBeMarked);
        TaskBook expectedAB = helper.generateAddressBook(Collections.emptyList(), deadlineList, Collections.emptyList());
        expectedAB.completeTask(toBeMarked);
        helper.addToModel(model, Collections.emptyList(), deadlineList, Collections.emptyList());

        assertCommandBehavior("done D3",
                Messages.MESSAGE_INVALID_TASK_DISPLAYED_INDEX,
                expectedAB,
                Collections.emptyList(),
                deadlineList,
                Collections.emptyList());
    }

    //@@author A0138993L
    @Test
    public void execute_done_noTodoIndex() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        Task toBeMarked = helper.charlie();
        List<Task> todoList = helper.generateTodoList(toBeMarked);
        TaskBook expectedAB = helper.generateAddressBook(Collections.emptyList(), Collections.emptyList(), todoList);
        expectedAB.completeTask(toBeMarked);
        helper.addToModel(model, Collections.emptyList(), Collections.emptyList(), todoList);

        assertCommandBehavior("done E3",
                Messages.MESSAGE_INVALID_TASK_DISPLAYED_INDEX,
                expectedAB,
                Collections.emptyList(),
                Collections.emptyList(),
                todoList);
    }
    /**
     * Confirms the 'invalid argument index number behaviour' for the given command
     * targeting a single person in the shown list, using visible index.
     * @param commandWord to test assuming it targets a single person in the last shown list based on visible index.
     */
    private void assertIncorrectIndexFormatBehaviorForCommand(String commandWord, String expectedMessage) throws Exception {
        assertCommandBehavior(commandWord , expectedMessage); //index missing
        assertCommandBehavior(commandWord + " +1", expectedMessage); //index should be unsigned
        assertCommandBehavior(commandWord + " -1", expectedMessage); //index should be unsigned
        assertCommandBehavior(commandWord + " 0", expectedMessage); //index cannot be 0
        assertCommandBehavior(commandWord + " 1", expectedMessage); //index cannot just be a number (must have E/D/T in front)
        assertCommandBehavior(commandWord + " not_a_number", expectedMessage);
    }

    /**
     * Confirms the 'invalid argument index number behaviour' for the given command
     * targeting a single person in the shown list, using visible index.
     * @param commandWord to test assuming it targets a single person in the last shown list based on visible index.
     */
    private void assertIndexNotFoundBehaviorForCommand(String commandWord) throws Exception {
        String expectedMessage = MESSAGE_INVALID_TASK_DISPLAYED_INDEX;
        TestDataHelper helper = new TestDataHelper();
        List<Task> eventList = helper.generateEventsList(2);
        List<Task> deadlineList = helper.generateDeadlineList(2);
        List<Task> todoList = helper.generateTodoList(2);

        // set AB state to 2 persons
        model.resetData(new TaskBook());
        for (Task p : eventList) {
            model.addTask(p);
        }
        for (Task p : deadlineList) {
            model.addTask(p);
        }
        for (Task p : todoList) {
            model.addTask(p);
        }

        assertCommandBehavior(commandWord , expectedMessage, model.getAddressBook(), eventList, deadlineList, todoList);
    }
    //@@author A0138993L
    @Test
    public void execute_selectInvalidArgsFormat_errorMessageShown() throws Exception {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, SelectCommand.MESSAGE_USAGE);
        assertIncorrectIndexFormatBehaviorForCommand("select", expectedMessage);
    }
    //@@author A0138993L
    @Test
    public void execute_selectEventIndexNotFound_errorMessageShown() throws Exception {
        assertIndexNotFoundBehaviorForCommand("select E6");
    }
    //@@author A0138993L
    @Test
    public void execute_selectDeadlineIndexNotFound_errorMessageShown() throws Exception {
        assertIndexNotFoundBehaviorForCommand("select D6");
    }
    //@@author A0138993L
    @Test
    public void execute_selectTodoIndexNotFound_errorMessageShown() throws Exception {
        assertIndexNotFoundBehaviorForCommand("select T6");
    }
    //@@author A0138993L
    @Test
    public void execute_select_jumpsToCorrectEvent() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        List<Task> threeEvents = helper.generateEventsList(3);

        TaskBook expectedAB = helper.generateAddressBook(threeEvents, Collections.emptyList(), Collections.emptyList());
        helper.addToModel(model, threeEvents, Collections.emptyList(), Collections.emptyList());

        assertCommandBehavior("select E2",
                String.format(SelectCommand.MESSAGE_SELECT_TASK_SUCCESS, threeEvents.get(1)),
                expectedAB,
                expectedAB.getEventList(),
                expectedAB.getDeadlineList(),
                expectedAB.getTodoList());
        assertEquals(1, targetedJumpIndex);
        assertEquals(model.getFilteredEventList().get(1), threeEvents.get(1));
    }
    //@@author A0138993L
    @Test
    public void execute_select_jumpsToCorrectDeadline() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        List<Task> threeEvents = helper.generateEventsList(3);
        List<Task> threeDeadlines = helper.generateDeadlineList(3);
        List<Task> threeTodos = helper.generateTodoList(3);

        TaskBook expectedAB = helper.generateAddressBook(threeEvents, threeDeadlines, threeTodos);
        helper.addToModel(model, threeEvents, threeDeadlines, threeTodos);

        assertCommandBehavior("select D2",
                String.format(SelectCommand.MESSAGE_SELECT_TASK_SUCCESS, threeDeadlines.get(1)),
                expectedAB,
                expectedAB.getEventList(),
                expectedAB.getDeadlineList(),
                expectedAB.getTodoList());
        assertEquals(1, targetedJumpIndex);
        assertEquals(model.getFilteredDeadlineList().get(1), threeDeadlines.get(1));
    }
    //@@author A0138993L
    @Test
    public void execute_select_jumpsToCorrectTodo() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        List<Task> threeEvents = helper.generateEventsList(3);
        List<Task> threeDeadlines = helper.generateDeadlineList(3);
        List<Task> threeTodos = helper.generateTodoList(3);

        TaskBook expectedAB = helper.generateAddressBook(threeEvents, threeDeadlines, threeTodos);
        helper.addToModel(model, threeEvents, threeDeadlines, threeTodos);

        assertCommandBehavior("select T2",
                String.format(SelectCommand.MESSAGE_SELECT_TASK_SUCCESS, threeTodos.get(1)),
                expectedAB,
                expectedAB.getEventList(),
                expectedAB.getDeadlineList(),
                expectedAB.getTodoList());
        assertEquals(1, targetedJumpIndex);
        assertEquals(model.getFilteredTodoList().get(1), threeTodos.get(1));
    }

    //@@author A0139430L JingRui
    @Test
    public void execute_deleteInvalidArgsFormat_errorMessageShown() throws Exception {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, DeleteCommand.MESSAGE_USAGE);
        assertIncorrectIndexFormatBehaviorForCommand("delete", expectedMessage);
    }

    @Test
    public void execute_deleteIndexNotFound_errorMessageShown() throws Exception {
        assertIndexNotFoundBehaviorForCommand("delete E5");
        assertIndexNotFoundBehaviorForCommand("delete D5");
        assertIndexNotFoundBehaviorForCommand("delete T5");
    }
    //@@author A0139430L JingRui
    @Test
    public void execute_delete_removesCorrectEvent() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        List<Task> threePersons = helper.generateEventsList(3);
        List<Task> threeDeadlines = helper.generateDeadlineList(3);
        List<Task> threeTodos = helper.generateTodoList(3);

        TaskBook expectedAB = helper.generateAddressBook(threePersons, threeDeadlines, threeTodos);
        expectedAB.removeTask(threePersons.get(1));

        helper.addToModel(model, threePersons, threeDeadlines, threeTodos);

        assertCommandBehavior("delete E2",
                String.format(DeleteCommand.MESSAGE_DELETE_TASK_SUCCESS, /*threePersons.get(1)*/ new String("[E2]")),
                expectedAB,
                expectedAB.getEventList(),
                expectedAB.getDeadlineList(),
                expectedAB.getTodoList());
    }
    //@@author A0139430L JingRui
    @Test
    public void execute_delete_removesCorrectDeadline() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        List<Task> threePersons = helper.generateEventsList(3);
        List<Task> threeDeadlines = helper.generateDeadlineList(3);
        List<Task> threeTodos = helper.generateTodoList(3);

        TaskBook expectedAB = helper.generateAddressBook(threePersons, threeDeadlines, threeTodos);
        expectedAB.removeTask(threeDeadlines.get(1));

        helper.addToModel(model, threePersons, threeDeadlines, threeTodos);

        assertCommandBehavior("delete D2",
                String.format(DeleteCommand.MESSAGE_DELETE_TASK_SUCCESS, /*threePersons.get(1)*/ new String("[D2]")),
                expectedAB,
                expectedAB.getEventList(),
                expectedAB.getDeadlineList(),
                expectedAB.getTodoList());
    }
    //@@author A0139430L JingRui
    @Test
    public void execute_delete_removesCorrectTodo() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        List<Task> threePersons = helper.generateEventsList(3);
        List<Task> threeDeadlines = helper.generateDeadlineList(3);
        List<Task> threeTodos = helper.generateTodoList(3);

        TaskBook expectedAB = helper.generateAddressBook(threePersons, threeDeadlines, threeTodos);
        expectedAB.removeTask(threeTodos.get(1));

        helper.addToModel(model, threePersons, threeDeadlines, threeTodos);

        assertCommandBehavior("delete T2",
                String.format(DeleteCommand.MESSAGE_DELETE_TASK_SUCCESS, /*threePersons.get(1)*/ new String("[T2]")),
                expectedAB,
                expectedAB.getEventList(),
                expectedAB.getDeadlineList(),
                expectedAB.getTodoList());
    }
    //@@author A0139430L JingRui
    @Test
    public void execute_delete_removesCorrectPersonsEventMultiple() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        List<Task> threePersons = helper.generateEventsList(3);
        List<Task> threeDeadlines = helper.generateDeadlineList(3);
        List<Task> threeTodos = helper.generateTodoList(3);

        TaskBook expectedAB = helper.generateAddressBook(threePersons, threeDeadlines, threeTodos);
        expectedAB.removeTask(threePersons.get(0));
        expectedAB.removeTask(threePersons.get(1));
        expectedAB.removeTask(threePersons.get(2));
        helper.addToModel(model, threePersons, threeDeadlines, threeTodos);

        assertCommandBehavior("delete E1, E2, E3",
                String.format(DeleteCommand.MESSAGE_DELETE_TASK_SUCCESS, /*threePersons.get(1)*/ new String("[E1, E2, E3]")),
                expectedAB,
                expectedAB.getEventList(),
                expectedAB.getDeadlineList(),
                expectedAB.getTodoList());
    }
    //@@author A0139430L JingRui
    @Test
    public void execute_delete_removesCorrectEventRange() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        List<Task> threePersons = helper.generateEventsList(3);
        List<Task> threeDeadlines = helper.generateDeadlineList(3);
        List<Task> threeTodos = helper.generateTodoList(3);

        TaskBook expectedAB = helper.generateAddressBook(threePersons, threeDeadlines, threeTodos);
        expectedAB.removeTask(threePersons.get(0));
        expectedAB.removeTask(threePersons.get(1));
        expectedAB.removeTask(threePersons.get(2));
        helper.addToModel(model, threePersons, threeDeadlines, threeTodos);

        assertCommandBehavior("delete E1-E3",
                String.format(DeleteCommand.MESSAGE_DELETE_TASK_SUCCESS, /*threePersons.get(1)*/ new String("[E1, E2, E3]")),
                expectedAB,
                expectedAB.getEventList(),
                expectedAB.getDeadlineList(),
                expectedAB.getTodoList());
    }
    //@@author A0139430L JingRui
    @Test
    public void execute_delete_removesCorrectDeadlineRange() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        List<Task> threePersons = helper.generateEventsList(3);
        List<Task> threeDeadlines = helper.generateDeadlineList(3);
        List<Task> threeTodos = helper.generateTodoList(3);

        TaskBook expectedAB = helper.generateAddressBook(threePersons, threeDeadlines, threeTodos);
        expectedAB.removeTask(threeDeadlines.get(0));
        expectedAB.removeTask(threeDeadlines.get(1));
        expectedAB.removeTask(threeDeadlines.get(2));
        helper.addToModel(model, threePersons, threeDeadlines, threeTodos);

        assertCommandBehavior("delete D1-D3",
                String.format(DeleteCommand.MESSAGE_DELETE_TASK_SUCCESS, /*threePersons.get(1)*/ new String("[D1, D2, D3]")),
                expectedAB,
                expectedAB.getEventList(),
                expectedAB.getDeadlineList(),
                expectedAB.getTodoList());
    }
    //@@author A0139430L JingRui
    @Test
    public void execute_delete_removesCorrectDeadlineMultiple() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        List<Task> threePersons = helper.generateEventsList(3);
        List<Task> threeDeadlines = helper.generateDeadlineList(3);
        List<Task> threeTodos = helper.generateTodoList(3);

        TaskBook expectedAB = helper.generateAddressBook(threePersons, threeDeadlines, threeTodos);
        expectedAB.removeTask(threeDeadlines.get(0));
        expectedAB.removeTask(threeDeadlines.get(1));
        expectedAB.removeTask(threeDeadlines.get(2));
        helper.addToModel(model, threePersons, threeDeadlines, threeTodos);

        assertCommandBehavior("delete D1, D2, D3",
                String.format(DeleteCommand.MESSAGE_DELETE_TASK_SUCCESS, /*threePersons.get(1)*/ new String("[D1, D2, D3]")),
                expectedAB,
                expectedAB.getEventList(),
                expectedAB.getDeadlineList(),
                expectedAB.getTodoList());
    }
    //@@author A0139430L JingRui
    @Test
    public void execute_delete_removesCorrectTodoRange() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        List<Task> threePersons = helper.generateEventsList(3);
        List<Task> threeDeadlines = helper.generateDeadlineList(3);
        List<Task> threeTodos = helper.generateTodoList(3);

        TaskBook expectedAB = helper.generateAddressBook(threePersons, threeDeadlines, threeTodos);
        expectedAB.removeTask(threeTodos.get(0));
        expectedAB.removeTask(threeTodos.get(1));
        expectedAB.removeTask(threeTodos.get(2));
        helper.addToModel(model, threePersons, threeDeadlines, threeTodos);

        assertCommandBehavior("delete T1-T3",
                String.format(DeleteCommand.MESSAGE_DELETE_TASK_SUCCESS, /*threePersons.get(1)*/ new String("[T1, T2, T3]")),
                expectedAB,
                expectedAB.getEventList(),
                expectedAB.getDeadlineList(),
                expectedAB.getTodoList());
    }
    //@@author A0139430L JingRui
    @Test
    public void execute_delete_removesCorrectTodoMultiple() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        List<Task> threePersons = helper.generateEventsList(3);
        List<Task> threeDeadlines = helper.generateDeadlineList(3);
        List<Task> threeTodos = helper.generateTodoList(3);

        TaskBook expectedAB = helper.generateAddressBook(threePersons, threeDeadlines, threeTodos);
        expectedAB.removeTask(threeTodos.get(0));
        expectedAB.removeTask(threeTodos.get(1));
        expectedAB.removeTask(threeTodos.get(2));
        helper.addToModel(model, threePersons, threeDeadlines, threeTodos);

        assertCommandBehavior("delete T1, T2, T3",
                String.format(DeleteCommand.MESSAGE_DELETE_TASK_SUCCESS, /*threePersons.get(1)*/ new String("[T1, T2, T3]")),
                expectedAB,
                expectedAB.getEventList(),
                expectedAB.getDeadlineList(),
                expectedAB.getTodoList());
    }
    //@@author A0139430L JingRui
    @Test
    public void execute_editInvalidArgsFormat_errorMessageShown() throws Exception {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, EditCommand.MESSAGE_USAGE);
        assertIncorrectIndexFormatBehaviorForCommand("edit", expectedMessage);
    }
    //@@author A0139430L JingRui
    @Test
    public void execute_editIndexNotFound_errorMessageShown() throws Exception {
        assertIndexNotFoundBehaviorForCommand("edit E5 des sadas");
        assertIndexNotFoundBehaviorForCommand("edit E5 date 121222");
        assertIndexNotFoundBehaviorForCommand("edit E5 start 1234");
        assertIndexNotFoundBehaviorForCommand("edit E5 end 2359");
        assertIndexNotFoundBehaviorForCommand("edit E5 tag love");
        assertIndexNotFoundBehaviorForCommand("edit E5 tag love>this");

        assertIndexNotFoundBehaviorForCommand("edit D5 des sadas");
        assertIndexNotFoundBehaviorForCommand("edit D5 date 121222");
        assertIndexNotFoundBehaviorForCommand("edit D5 start 1234");
        assertIndexNotFoundBehaviorForCommand("edit D5 end 2359");
        assertIndexNotFoundBehaviorForCommand("edit D5 tag love");
        assertIndexNotFoundBehaviorForCommand("edit D5 tag love>this");

        assertIndexNotFoundBehaviorForCommand("edit T5 des sadas");
        assertIndexNotFoundBehaviorForCommand("edit T5 date 121222");
        assertIndexNotFoundBehaviorForCommand("edit T5 start 1234");
        assertIndexNotFoundBehaviorForCommand("edit T5 end 2359");
        assertIndexNotFoundBehaviorForCommand("edit T5 tag love");
        assertIndexNotFoundBehaviorForCommand("edit T5 tag love>this");
    }
    //@@author A0139430L JingRui
    @Test
    public void execute_edit_editCorrectEventName() throws Exception {      
        TestDataHelper helper = new TestDataHelper();
        List<Task> threePersons = helper.generateEventsList(3);
        List<Task> threeDeadlines = helper.generateDeadlineList(3);
        List<Task> threeTodos = helper.generateTodoList(3);

        TaskBook expectedAB = helper.generateAddressBook(threePersons, threeDeadlines, threeTodos);
        helper.addToModel(model, threePersons, threeDeadlines, threeTodos);

        UnmodifiableObservableList<ReadOnlyTask> lastShownEventList = model.getFilteredEventList();
        ReadOnlyTask eventToEdit = lastShownEventList.get(1-1);
        expectedAB.changeTask(eventToEdit, "des BEACH parTy" , 'E');

        assertCommandBehavior("edit E1 des BEACH parTy",
                String.format(EditCommand.MESSAGE_EDIT_TASK_SUCCESS, "E", "1", "des BEACH parTy"),
                expectedAB,
                expectedAB.getEventList(),
                expectedAB.getDeadlineList(),
                expectedAB.getTodoList());
    }
    //@@author A0139430L JingRui
    @Test
    public void execute_edit_editCorrectEventDate() throws Exception {      
        TestDataHelper helper = new TestDataHelper();
        List<Task> threePersons = helper.generateEventsList(3);
        List<Task> threeDeadlines = helper.generateDeadlineList(3);
        List<Task> threeTodos = helper.generateTodoList(3);

        TaskBook expectedAB = helper.generateAddressBook(threePersons, threeDeadlines, threeTodos);
        helper.addToModel(model, threePersons, threeDeadlines, threeTodos);

        UnmodifiableObservableList<ReadOnlyTask> lastShownEventList = model.getFilteredEventList();
        ReadOnlyTask eventToEdit = lastShownEventList.get(1-1);
        expectedAB.changeTask(eventToEdit, "date 12-12-17" , 'E');

        assertCommandBehavior("edit E1 date 12-12-17",
                String.format(EditCommand.MESSAGE_EDIT_TASK_SUCCESS, "E", "1", "date 12-12-17"),
                expectedAB,
                expectedAB.getEventList(),
                expectedAB.getDeadlineList(),
                expectedAB.getTodoList());
    }
    //@@author A0139430L JingRui
    @Test
    public void execute_edit_editCorrectEventStart() throws Exception {      
        TestDataHelper helper = new TestDataHelper();
        List<Task> threePersons = helper.generateEventsList(3);
        List<Task> threeDeadlines = helper.generateDeadlineList(3);
        List<Task> threeTodos = helper.generateTodoList(3);

        TaskBook expectedAB = helper.generateAddressBook(threePersons, threeDeadlines, threeTodos);
        helper.addToModel(model, threePersons, threeDeadlines, threeTodos);

        UnmodifiableObservableList<ReadOnlyTask> lastShownEventList = model.getFilteredEventList();
        ReadOnlyTask eventToEdit = lastShownEventList.get(1-1);
        expectedAB.changeTask(eventToEdit, "start 5.00pm" , 'E');

        assertCommandBehavior("edit E1 start 5.00pm",
                String.format(EditCommand.MESSAGE_EDIT_TASK_SUCCESS, "E", "1", "start 5.00pm"),
                expectedAB,
                expectedAB.getEventList(),
                expectedAB.getDeadlineList(),
                expectedAB.getTodoList());
    }
    //@@author A0139430L JingRui
    @Test
    public void execute_edit_editCorrectEventEnd() throws Exception {      
        TestDataHelper helper = new TestDataHelper();
        List<Task> threePersons = helper.generateEventsList(3);
        List<Task> threeDeadlines = helper.generateDeadlineList(3);
        List<Task> threeTodos = helper.generateTodoList(3);

        TaskBook expectedAB = helper.generateAddressBook(threePersons, threeDeadlines, threeTodos);
        helper.addToModel(model, threePersons, threeDeadlines, threeTodos);

        UnmodifiableObservableList<ReadOnlyTask> lastShownEventList = model.getFilteredEventList();
        ReadOnlyTask eventToEdit = lastShownEventList.get(1-1);
        expectedAB.changeTask(eventToEdit, "end 5.00pm" , 'E');

        assertCommandBehavior("edit E1 end 5.00pm",
                String.format(EditCommand.MESSAGE_EDIT_TASK_SUCCESS, "E", "1", "end 5.00pm"),
                expectedAB,
                expectedAB.getEventList(),
                expectedAB.getDeadlineList(),
                expectedAB.getTodoList());
    }
    //@@author A0139430L JingRui
    @Test
    public void execute_edit_editCorrectEventSpecificTag() throws Exception {      
        TestDataHelper helper = new TestDataHelper();
        List<Task> threePersons = helper.generateEventsList(3);
        List<Task> threeDeadlines = helper.generateDeadlineList(3);
        List<Task> threeTodos = helper.generateTodoList(3);

        TaskBook expectedAB = helper.generateAddressBook(threePersons, threeDeadlines, threeTodos);
        helper.addToModel(model, threePersons, threeDeadlines, threeTodos);

        UnmodifiableObservableList<ReadOnlyTask> lastShownEventList = model.getFilteredEventList();
        ReadOnlyTask eventToEdit = lastShownEventList.get(1-1);
        expectedAB.changeTask(eventToEdit, "tag tag1>anything" , 'E');

        assertCommandBehavior("edit E1 tag tag1>anything",
                String.format(EditCommand.MESSAGE_EDIT_TASK_SUCCESS, "E", "1", "tag tag1>anything"),
                expectedAB,
                expectedAB.getEventList(),
                expectedAB.getDeadlineList(),
                expectedAB.getTodoList());
    }
    //@@author A0139430L JingRui
    @Test
    public void execute_edit_editCorrectEventTag() throws Exception {      
        TestDataHelper helper = new TestDataHelper();
        List<Task> threePersons = helper.generateEventsList(3);
        List<Task> threeDeadlines = helper.generateDeadlineList(3);
        List<Task> threeTodos = helper.generateTodoList(3);

        TaskBook expectedAB = helper.generateAddressBook(threePersons, threeDeadlines, threeTodos);
        helper.addToModel(model, threePersons, threeDeadlines, threeTodos);

        UnmodifiableObservableList<ReadOnlyTask> lastShownEventList = model.getFilteredEventList();
        ReadOnlyTask eventToEdit = lastShownEventList.get(1-1);
        expectedAB.changeTask(eventToEdit, "tag anything" , 'E');

        assertCommandBehavior("edit E1 tag anything",
                String.format(EditCommand.MESSAGE_EDIT_TASK_SUCCESS, "E", "1", "tag anything"),
                expectedAB,
                expectedAB.getEventList(),
                expectedAB.getDeadlineList(),
                expectedAB.getTodoList());
    }
    //@@author A0139430L JingRui
    @Test
    public void execute_add_invalidTagFormat() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        List<Task> threePersons = helper.generateEventsList(3);
        List<Task> threeDeadlines = helper.generateDeadlineList(3);
        List<Task> threeTodos = helper.generateTodoList(3);

        TaskBook expectedAB = helper.generateAddressBook(threePersons, threeDeadlines, threeTodos);
        helper.addToModel(model, threePersons, threeDeadlines, threeTodos);

        assertCommandBehavior("add T1 #.", Tag.MESSAGE_TAG_CONSTRAINTS,
                expectedAB,
                expectedAB.getEventList(),
                expectedAB.getDeadlineList(),
                expectedAB.getTodoList());
    }

    //@@author A0139430L JingRui
    @Test
    public void execute_edit_addTagSingle() throws Exception {      
        TestDataHelper helper = new TestDataHelper();
        List<Task> threePersons = helper.generateEventsList(3);
        List<Task> threeDeadlines = helper.generateDeadlineList(3);
        List<Task> threeTodos = helper.generateTodoList(3);

        TaskBook expectedAB = helper.generateAddressBook(threePersons, threeDeadlines, threeTodos);
        helper.addToModel(model, threePersons, threeDeadlines, threeTodos);

        UnmodifiableObservableList<ReadOnlyTask> lastShownEventList = model.getFilteredEventList();
        ReadOnlyTask eventToEdit = lastShownEventList.get(1-1);
        expectedAB.changeTask(eventToEdit, "add anything" , 'E');

        assertCommandBehavior("add E1 #anything",
                String.format(EditCommand.MESSAGE_EDIT_TASK_SUCCESS, "E", "1", "add #anything"),
                expectedAB,
                expectedAB.getEventList(),
                expectedAB.getDeadlineList(),
                expectedAB.getTodoList());
    }
    //@@author A0139430L JingRui
    @Test
    public void execute_edit_addTagMultiple() throws Exception {      
        TestDataHelper helper = new TestDataHelper();
        List<Task> threePersons = helper.generateEventsList(3);
        List<Task> threeDeadlines = helper.generateDeadlineList(3);
        List<Task> threeTodos = helper.generateTodoList(3);

        TaskBook expectedAB = helper.generateAddressBook(threePersons, threeDeadlines, threeTodos);
        helper.addToModel(model, threePersons, threeDeadlines, threeTodos);

        UnmodifiableObservableList<ReadOnlyTask> lastShownEventList = model.getFilteredEventList();
        ReadOnlyTask eventToEdit = lastShownEventList.get(1-1);
        expectedAB.changeTask(eventToEdit, "add anything something" , 'E');

        assertCommandBehavior("add E1 #anything #something",
                String.format(EditCommand.MESSAGE_EDIT_TASK_SUCCESS, "E", "1", "add #anything #something"),
                expectedAB,
                expectedAB.getEventList(),
                expectedAB.getDeadlineList(),
                expectedAB.getTodoList());
    }
    //@@author A0139430L JingRui
    @Test
    public void execute_edit_editCorrectDeadlineName() throws Exception {      
        TestDataHelper helper = new TestDataHelper();
        List<Task> threePersons = helper.generateEventsList(3);
        List<Task> threeDeadlines = helper.generateDeadlineList(3);
        List<Task> threeTodos = helper.generateTodoList(3);

        TaskBook expectedAB = helper.generateAddressBook(threePersons, threeDeadlines, threeTodos);
        helper.addToModel(model, threePersons, threeDeadlines, threeTodos);

        UnmodifiableObservableList<ReadOnlyTask> lastShownEventList = model.getFilteredDeadlineList();
        ReadOnlyTask eventToEdit = lastShownEventList.get(1-1);
        expectedAB.changeTask(eventToEdit, "des BEACH parTy" , 'D');

        assertCommandBehavior("edit D1 des BEACH parTy",
                String.format(EditCommand.MESSAGE_EDIT_TASK_SUCCESS, "D", "1", "des BEACH parTy"),
                expectedAB,
                expectedAB.getEventList(),
                expectedAB.getDeadlineList(),
                expectedAB.getTodoList());
    }
    //@@author A0139430L JingRui
    @Test
    public void execute_edit_editCorrectTodoName() throws Exception {      
        TestDataHelper helper = new TestDataHelper();
        List<Task> threePersons = helper.generateEventsList(3);
        List<Task> threeDeadlines = helper.generateDeadlineList(3);
        List<Task> threeTodos = helper.generateTodoList(3);

        TaskBook expectedAB = helper.generateAddressBook(threePersons, threeDeadlines, threeTodos);
        helper.addToModel(model, threePersons, threeDeadlines, threeTodos);

        UnmodifiableObservableList<ReadOnlyTask> lastShownEventList = model.getFilteredTodoList();
        ReadOnlyTask eventToEdit = lastShownEventList.get(1-1);
        expectedAB.changeTask(eventToEdit, "des BEACH parTy" , 'T');

        assertCommandBehavior("edit T1 des BEACH parTy",
                String.format(EditCommand.MESSAGE_EDIT_TASK_SUCCESS, "T", "1", "des BEACH parTy"),
                expectedAB,
                expectedAB.getEventList(),
                expectedAB.getDeadlineList(),
                expectedAB.getTodoList());
    }


    @Test
    public void execute_find_invalidArgsFormat() throws Exception {
        String expectedMessage = String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE);
        assertCommandBehavior("find ", expectedMessage);
    }
    //@@author A0138993L
    @Test
    public void execute_find_onlyMatchesPartialWordsInNames() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        Task pTarget1 = helper.generateEventWithName("bla bla KEY bla");
        Task pTarget2 = helper.generateEventWithName("KEYKEYKEY sduauo");
        Task p1 = helper.generateEventWithName("KE Y");
        Task p2 = helper.generateEventWithName("blaK EY bla bceofeia");

        List<Task> fourPersons = helper.generateEventList(p1, p2, pTarget1, pTarget2);
        List<Task> expectedList = helper.generateEventList(pTarget1, pTarget2);

        Task DTarget1 = helper.generateDeadlineWithName("bla KEY bla");
        Task DTarget2 = helper.generateDeadlineWithName("KEYKEY sduauo");
        Task D1 = helper.generateDeadlineWithName("K EY");
        Task D2 = helper.generateDeadlineWithName("blaK EY bla bcfeia");

        List<Task> fourDeadlines = helper.generateDeadlineList(D1, D2, DTarget1, DTarget2);
        List<Task> expectedDeadlineList = helper.generateDeadlineList(DTarget1, DTarget2);

        Task TTarget1 = helper.generateTodoWithName("bla bla KEY");
        Task TTarget2 = helper.generateTodoWithName("KEYKEYKEY sduo");
        Task T1 = helper.generateTodoWithName("KE Y dfd");
        Task T2 = helper.generateTodoWithName("blaK EYceofeia");

        List<Task> fourTodos = helper.generateTodoList(T1, T2, TTarget1, TTarget2);
        List<Task> expectedTodoList = helper.generateTodoList(TTarget1, TTarget2);
        helper.addToModel(model, fourPersons, fourDeadlines, fourTodos);

        TaskBook expectedAB = helper.generateAddressBook(fourPersons, fourDeadlines, fourTodos);

        assertCommandBehavior("find KEY",
                Command.getMessageForTaskListShownSummary(expectedList.size(), expectedDeadlineList.size(), expectedTodoList.size()),
                expectedAB,
                expectedList,
                expectedDeadlineList,
                expectedTodoList);
    }

    @Test
    public void execute_find_isNotCaseSensitive() throws Exception {
        TestDataHelper helper = new TestDataHelper();
        Task p1 = helper.generateEventWithName("bla bla KEY bla");
        Task p2 = helper.generateEventWithName("bla KEY bla bceofeia");
        Task p3 = helper.generateEventWithName("key key");
        Task p4 = helper.generateEventWithName("KEy sduauo");

        Task d1 = helper.generateDeadlineWithName("bla KEY bla");
        Task d2 = helper.generateDeadlineWithName("bla KEY bceofeia");
        Task d3 = helper.generateDeadlineWithName("ke key");
        Task d4 = helper.generateDeadlineWithName("KEy uauo");

        Task t1 = helper.generateTodoWithName("blla KEY bla");
        Task t2 = helper.generateTodoWithName("bla KEYofeia");
        Task t3 = helper.generateTodoWithName("key");
        Task t4 = helper.generateTodoWithName("KEy skeyduauo");

        List<Task> fourEvents = helper.generateEventList(p3, p1, p4, p2);
        List<Task> fourDeadlines = helper.generateDeadlineList(d3, d1, d4, d2);
        List<Task> fourTodos = helper.generateTodoList(t3, t1, t4, t2);
        TaskBook expectedAB = helper.generateAddressBook(fourEvents, fourDeadlines, fourTodos);
        List<Task> expectedList = fourEvents;
        List<Task> expectedDeadlineList = fourDeadlines;
        List<Task> expectedTodoList = fourTodos;
        helper.addToModel(model, fourEvents, fourDeadlines, fourTodos);

        assertCommandBehavior("find KEY",
                Command.getMessageForTaskListShownSummary(expectedList.size(), expectedDeadlineList.size(), expectedTodoList.size()),
                expectedAB,
                expectedList,
                expectedDeadlineList,
                expectedTodoList);
    }

    /**
     * A utility class to generate test data.
     */
    class TestDataHelper{

        Task adam() throws Exception {
            Name name = new Name("Events");
            Date date = new Date("12.12.16");
            Start start = new Start("2am");
            End end = new End("3pm");
            int task_cat = 1;
            int overdue =0;
            boolean isCompleted = false;
            Tag tag1 = new Tag("tag1");
            Tag tag2 = new Tag("tag2");
            UniqueTagList tags = new UniqueTagList(tag1, tag2);
            return new Task(name, date, start, end, task_cat, overdue, isCompleted, tags);
        }
        Task beta() throws Exception {
            Name name = new Name("Deadlines");
            Date date = new Date("12.12.16");
            Start start = new Start("no start");
            End end = new End("3pm");
            int task_cat = 2;
            int overdue =0;
            boolean isCompleted = false;
            Tag tag1 = new Tag("tag1");
            Tag tag2 = new Tag("tag2");
            UniqueTagList tags = new UniqueTagList(tag1, tag2);
            return new Task(name, date, start, end, task_cat, overdue, isCompleted, tags);
        }
        Task charlie() throws Exception {
            Name name = new Name("Todo");
            Date date = new Date("no date");
            Start start = new Start("no start");
            End end = new End("no end");
            int task_cat = 3;
            int overdue =0;
            boolean isCompleted = false;
            Tag tag1 = new Tag("tag1");
            Tag tag2 = new Tag("tag2");
            UniqueTagList tags = new UniqueTagList(tag1, tag2);
            return new Task(name, date, start, end, task_cat, overdue, isCompleted, tags);
        }

        /**
         * Generates a valid person using the given seed.
         * Running this function with the same parameter values guarantees the returned person will have the same state.
         * Each unique seed will generate a unique Task object.
         *
         * @param seed used to generate the person data field values
         */
        Task generatePerson(int seed) throws Exception {
            return new Task(
                    new Name("Task " + seed),
                    new Date("12122" + Math.abs(seed)),
                    new Start("125" + seed),
                    new End("235" + seed),
                    1,
                    0,
                    false,
                    new UniqueTagList(new Tag("tag" + Math.abs(seed)), new Tag("tag" + Math.abs(seed + 1)))
                    );
        }

        /**
         * @@author A0138993L
         * Generates a valid deadline using the given seed.
         * Running this function with the same parameter values guarantees the returned deadline will have the same state.
         * Each unique seed will generate a unique Task object.
         *
         * @param seed used to generate the deadline data field values
         */
        Task generateDeadline(int seed) throws Exception {
            return new Task(
                    new Name("DTask " + seed),
                    new Date("12122" + Math.abs(seed)),
                    new Start("no start"),
                    new End("235" + seed),
                    2,
                    0,
                    false,
                    new UniqueTagList(new Tag("tag" + Math.abs(seed)), new Tag("tag" + Math.abs(seed + 1)))
                    );
        }
        /**
         * @@author A0138993L
         * Generates a valid deadline using the given seed.
         * Running this function with the same parameter values guarantees the returned deadline will have the same state.
         * Each unique seed will generate a unique Task object.
         *
         * @param seed used to generate the deadline data field values
         */
        Task generateTodo(int seed) throws Exception {
            return new Task(
                    new Name("TTask " + seed),
                    new Date("no date"),
                    new Start("no start"),
                    new End("no end"),
                    3,
                    0,
                    false,
                    new UniqueTagList(new Tag("tag" + Math.abs(seed)), new Tag("tag" + Math.abs(seed + 1)))
                    );
        }

        /** Generates the correct add command based on the person given */
        //@@author A0138993L
        String generateAddCommand(Task p) {
            StringBuffer cmd = new StringBuffer();

            cmd.append("add ");
            cmd.append("[").append(p.getName().toString());
            cmd.append("; ").append(p.getDate());
            cmd.append("; ").append(p.getStart());
            cmd.append("; ").append(p.getEnd());
            cmd.append("]");
            UniqueTagList tags = p.getTags();
            for(Tag t: tags){
                cmd.append(" #").append(t.tagName);
            }
            System.out.println(cmd);
            return cmd.toString();
        }

        /** Generates the correct add command based on the deadline given */
        //@@author A0138993L
        String generateAddDeadlineCommand(Task p) {
            StringBuffer cmd = new StringBuffer();

            cmd.append("add ");
            cmd.append(p.getName().toString());
            cmd.append("; ").append(p.getDate());
            cmd.append("; ").append(p.getEnd());
            UniqueTagList tags = p.getTags();
            for(Tag t: tags){
                cmd.append(" #").append(t.tagName);
            }
            System.out.println(cmd);
            return cmd.toString();
        }

        /** Generates the correct add command based on the todo given */
        //@@author A0138993L
        String generateAddTodoCommand(Task p) {
            StringBuffer cmd = new StringBuffer();

            cmd.append("add ");
            cmd.append(p.getName().toString());
            UniqueTagList tags = p.getTags();
            for(Tag t: tags){
                cmd.append(" #").append(t.tagName);
            }
            System.out.println(cmd);

            return cmd.toString();
        }

        /**
         * @@author A0138993L
         * Generates an AddressBook with auto-generated persons.
         */
        TaskBook generateAddressBook(int numGenerated, int numGenerated2, int numGenerated3) throws Exception{
            TaskBook addressBook = new TaskBook();
            addToAddressBook(addressBook, numGenerated, numGenerated2, numGenerated3);
            return addressBook;
        }

        /**
         * @@author A0138993L
         * Generates an AddressBook based on the list of Persons given.
         */
        TaskBook generateAddressBook(List<Task> persons, List<Task> deadlines, List<Task> todos) throws Exception{
            TaskBook addressBook = new TaskBook();
            addToAddressBook(addressBook, persons, deadlines, todos);
            return addressBook;
        }

        /**
         * Adds auto-generated Task objects to the given AddressBook
         * @param addressBook The AddressBook to which the Persons will be added
         */
        void addToAddressBook(TaskBook addressBook, int numGenerated, int numGenerated2, int numGenerated3) throws Exception{
            addToAddressBook(addressBook, generateEventsList(numGenerated), generateDeadlineList(numGenerated2), generateTodoList(numGenerated3));
        }

        /**
         * Adds the given list of Persons to the given AddressBook
         */
        void addToAddressBook(TaskBook addressBook, List<Task> personsToAdd, List<Task> deadlinesToAdd, List<Task> todoToAdd) throws Exception{
            for(Task p: personsToAdd){
                addressBook.addTask(p);
            }
            for(Task p: deadlinesToAdd){
                addressBook.addTask(p);
            }
            for(Task p: todoToAdd){
                addressBook.addTask(p);
            }
        }

        /**
         * Adds auto-generated Task objects to the given model
         * @param model The model to which the Persons will be added
         */
        void addToModel(Model model, int numGenerated, int numGenerated2, int numGenerated3) throws Exception{
            addToModel(model, generateEventsList(numGenerated), generateDeadlineList(numGenerated2), generateTodoList(numGenerated3));
        }

        /**
         * Adds the given list of Persons to the given model
         */
        void addToModel(Model model, List<Task> personsToAdd, List<Task> deadlinesToAdd, List<Task> todosToAdd) throws Exception{
            for(Task p: personsToAdd){
                model.addTask(p);
            }
            for(Task p: deadlinesToAdd){
                model.addTask(p);
            }
            for(Task p: todosToAdd){
                model.addTask(p);
            }

        }

        /**
         * Generates a list of Persons based on the flags.
         */
        List<Task> generateEventsList(int numGenerated) throws Exception{
            List<Task> persons = new ArrayList<>();
            for(int i = 1; i <= numGenerated; i++){
                persons.add(generatePerson(i));
            }
            return persons;
        }

        List<Task> generateEventList(Task... persons) {
            return Arrays.asList(persons);
        }

        /*
         * Generates a list of deadlines based on flags.
         */
        List<Task> generateDeadlineList(int numGenerated) throws Exception{
            List<Task> deadlines = new ArrayList<>();
            for(int i = 1; i <= numGenerated; i++){
                deadlines.add(generateDeadline(i));
            }
            return deadlines;
        }

        List<Task> generateDeadlineList(Task... deadlines) {
            return Arrays.asList(deadlines);
        }

        /*
         * Generates a list of todos based on flags.
         */
        List<Task> generateTodoList(int numGenerated) throws Exception{
            List<Task> todos = new ArrayList<>();
            for(int i = 1; i <= numGenerated; i++){
                todos.add(generateTodo(i));
            }
            return todos;
        }

        List<Task> generateTodoList(Task... todo) {
            return Arrays.asList(todo);
        }

        /**
         * Generates a Task event object with given name. Other fields will have some dummy values.
         */
        Task generateEventWithName(String name) throws Exception {
            return new Task(
                    new Name(name),
                    new Date("31/12/16"),
                    new Start("12.21am"),
                    new End("1300"),
                    1,
                    0,
                    false,
                    new UniqueTagList(new Tag("tag"))
                    );
        }
        /**
         * Generates a Task event object with given date. Other fields will have some dummy values.
         */
        Task generateEventWithDate(String date) throws Exception {
            return new Task(
                    new Name("Event"),
                    new Date(date),
                    new Start("12.21am"),
                    new End("1300"),
                    1,
                    0,
                    false,
                    new UniqueTagList(new Tag("tag"))
                    );
        }
        /**
         * Generates a Task event object with given date. Other fields will have some dummy values.
         */
        Task generateEventWithStart(String start) throws Exception {
            return new Task(
                    new Name("Event"),
                    new Date("31/12/16"),
                    new Start(start),
                    new End("1300"),
                    1,
                    0,
                    false,
                    new UniqueTagList(new Tag("tag"))
                    );
        }
        /**
         * Generates a Task event object with given date. Other fields will have some dummy values.
         */
        Task generateEventWithEnd(String end) throws Exception {
            return new Task(
                    new Name("Event"),
                    new Date("31/12/16"),
                    new Start("12.21am"),
                    new End("end"),
                    1,
                    0,
                    false,
                    new UniqueTagList(new Tag("tag"))
                    );
        }
        /**
         * Generates a Task event object with given date. Other fields will have some dummy values.
         */
        Task generateEventWithTag(String tag) throws Exception {
            return new Task(
                    new Name("Event"),
                    new Date("31/12/16"),
                    new Start("12.21am"),
                    new End("1300"),
                    1,
                    0,
                    false,
                    new UniqueTagList(new Tag(tag))
                    );
        }

        /**
         * Generates a Task deadline object with given name. Other fields will have some dummy values.
         */
        Task generateDeadlineWithName(String name) throws Exception {
            return new Task(
                    new Name(name),
                    new Date("31/12/16"),
                    new Start("no start"),
                    new End("1300"),
                    2,
                    0,
                    false,
                    new UniqueTagList(new Tag("tag"))
                    );
        }

        /**
         * Generates a Task todo object with given name. Other fields will have some dummy values.
         */
        Task generateTodoWithName(String name) throws Exception {
            return new Task(
                    new Name(name),
                    new Date("no date"),
                    new Start("no start"),
                    new End("no end"),
                    3,
                    0,
                    false,
                    new UniqueTagList(new Tag("tag"))
                    );
        }


    }
}
