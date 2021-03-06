package seedu.simply.testutil;

import com.google.common.io.Files;

import guitests.guihandles.DeadlineCardHandle;
import guitests.guihandles.PersonCardHandle;
import guitests.guihandles.TodoCardHandle;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import junit.framework.AssertionFailedError;
import org.loadui.testfx.GuiTest;
import org.testfx.api.FxToolkit;

import seedu.simply.TestApp;
import seedu.simply.commons.exceptions.IllegalValueException;
import seedu.simply.commons.util.FileUtil;
import seedu.simply.commons.util.XmlUtil;
import seedu.simply.model.TaskBook;
import seedu.simply.model.tag.Tag;
import seedu.simply.model.tag.UniqueTagList;
import seedu.simply.model.task.*;
import seedu.simply.storage.XmlSerializableTaskBook;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * A utility class for test cases.
 */
public class TestUtil {

    public static String LS = System.lineSeparator();

    public static void assertThrows(Class<? extends Throwable> expected, Runnable executable) {
        try {
            executable.run();
        }
        catch (Throwable actualException) {
            if (!actualException.getClass().isAssignableFrom(expected)) {
                String message = String.format("Expected thrown: %s, actual: %s", expected.getName(),
                        actualException.getClass().getName());
                throw new AssertionFailedError(message);
            } else return;
        }
        throw new AssertionFailedError(
                String.format("Expected %s to be thrown, but nothing was thrown.", expected.getName()));
    }

    /**
     * Folder used for temp files created during testing. Ignored by Git.
     */
    public static String SANDBOX_FOLDER = FileUtil.getPath("./src/test/data/sandbox/");

    public static final Task[] samplePersonData = getSamplePersonData();

    private static Task[] getSamplePersonData() {
        try {
            return new Task[]{
                    new Task(new Name("Event1"), new Date("121216"), new Start("1212"), new End("1234"), 1, 0, false, new UniqueTagList()),
                    new Task(new Name("Event2"), new Date("12.12.16"), new Start("0001"), new End("1111"), 1, 0, false, new UniqueTagList()),
                    new Task(new Name("Event3"), new Date("10-02-16"), new Start("1242"), new End("2345"), 1, 0, false, new UniqueTagList()),
                    new Task(new Name("Event4"), new Date("300516"), new Start("1922"), new End("2021"), 1, 0, false, new UniqueTagList()),
                    new Task(new Name("Event5"), new Date("12/12/16"), new Start("1030"), new End("1242"), 1, 0, false, new UniqueTagList()),
                    new Task(new Name("Event6"), new Date("12/12/16"), new Start("12.35am"), new End("2234"), 1, 0, false, new UniqueTagList()),
                    new Task(new Name("Event7"), new Date("300116"), new Start("1235"), new End("1634"), 1, 0, false, new UniqueTagList()),
                    new Task(new Name("Event8"), new Date("13-12-16"), new Start("1245"), new End("1612"), 1, 0, false, new UniqueTagList()),
                    new Task(new Name("Event9"), new Date("120126"), new Start("0000"), new End("1212"), 1, 0, false, new UniqueTagList())
            };
        } catch (IllegalValueException e) {
            assert false;
            //not possible
            return null;
        }
    }

    public static final Tag[] sampleTagData = getSampleTagData();

    private static Tag[] getSampleTagData() {
        try {
            return new Tag[]{
                    new Tag("relatives"),
                    new Tag("friends")
            };
        } catch (IllegalValueException e) {
            assert false;
            return null;
            //not possible
        }
    }

    public static List<Task> generateSamplePersonData() {
        return Arrays.asList(samplePersonData);
    }

    /**
     * Appends the file name to the sandbox folder path.
     * Creates the sandbox folder if it doesn't exist.
     * @param fileName
     * @return
     */
    public static String getFilePathInSandboxFolder(String fileName) {
        try {
            FileUtil.createDirs(new File(SANDBOX_FOLDER));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return SANDBOX_FOLDER + fileName;
    }

    public static void createDataFileWithSampleData(String filePath) {
        createDataFileWithData(generateSampleStorageTaskBook(), filePath);
    }

    public static <T> void createDataFileWithData(T data, String filePath) {
        try {
            File saveFileForTesting = new File(filePath);
            FileUtil.createIfMissing(saveFileForTesting);
            XmlUtil.saveDataToFile(saveFileForTesting, data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String... s) {
        createDataFileWithSampleData(TestApp.SAVE_LOCATION_FOR_TESTING);
    }

    public static TaskBook generateEmptyTaskBook() {
        return new TaskBook(new UniqueTaskList(), new UniqueTaskList(), new UniqueTaskList(), new UniqueTagList());
    }

    public static XmlSerializableTaskBook generateSampleStorageTaskBook() {
        return new XmlSerializableTaskBook(generateEmptyTaskBook());
    }

    /**
     * Tweaks the {@code keyCodeCombination} to resolve the {@code KeyCode.SHORTCUT} to their
     * respective platform-specific keycodes
     */
    public static KeyCode[] scrub(KeyCodeCombination keyCodeCombination) {
        List<KeyCode> keys = new ArrayList<>();
        if (keyCodeCombination.getAlt() == KeyCombination.ModifierValue.DOWN) {
            keys.add(KeyCode.ALT);
        }
        if (keyCodeCombination.getShift() == KeyCombination.ModifierValue.DOWN) {
            keys.add(KeyCode.SHIFT);
        }
        if (keyCodeCombination.getMeta() == KeyCombination.ModifierValue.DOWN) {
            keys.add(KeyCode.META);
        }
        if (keyCodeCombination.getControl() == KeyCombination.ModifierValue.DOWN) {
            keys.add(KeyCode.CONTROL);
        }
        keys.add(keyCodeCombination.getCode());
        return keys.toArray(new KeyCode[]{});
    }

    public static boolean isHeadlessEnvironment() {
        String headlessProperty = System.getProperty("testfx.headless");
        return headlessProperty != null && headlessProperty.equals("true");
    }

    public static void captureScreenShot(String fileName) {
        File file = GuiTest.captureScreenshot();
        try {
            Files.copy(file, new File(fileName + ".png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String descOnFail(Object... comparedObjects) {
        return "Comparison failed \n"
                + Arrays.asList(comparedObjects).stream()
                .map(Object::toString)
                .collect(Collectors.joining("\n"));
    }

    public static void setFinalStatic(Field field, Object newValue) throws NoSuchFieldException, IllegalAccessException{
        field.setAccessible(true);
        // remove final modifier from field
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        // ~Modifier.FINAL is used to remove the final modifier from field so that its value is no longer
        // final and can be changed
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, newValue);
    }

    public static void initRuntime() throws TimeoutException {
        FxToolkit.registerPrimaryStage();
        FxToolkit.hideStage();
    }

    public static void tearDownRuntime() throws Exception {
        FxToolkit.cleanupStages();
    }

    /**
     * Gets private method of a class
     * Invoke the method using method.invoke(objectInstance, params...)
     *
     * Caveat: only find method declared in the current Class, not inherited from supertypes
     */
    public static Method getPrivateMethod(Class objectClass, String methodName) throws NoSuchMethodException {
        Method method = objectClass.getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method;
    }

    public static void renameFile(File file, String newFileName) {
        try {
            Files.copy(file, new File(newFileName));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Gets mid point of a node relative to the screen.
     * @param node
     * @return
     */
    public static Point2D getScreenMidPoint(Node node) {
        double x = getScreenPos(node).getMinX() + node.getLayoutBounds().getWidth() / 2;
        double y = getScreenPos(node).getMinY() + node.getLayoutBounds().getHeight() / 2;
        return new Point2D(x,y);
    }

    /**
     * Gets mid point of a node relative to its scene.
     * @param node
     * @return
     */
    public static Point2D getSceneMidPoint(Node node) {
        double x = getScenePos(node).getMinX() + node.getLayoutBounds().getWidth() / 2;
        double y = getScenePos(node).getMinY() + node.getLayoutBounds().getHeight() / 2;
        return new Point2D(x,y);
    }

    /**
     * Gets the bound of the node relative to the parent scene.
     * @param node
     * @return
     */
    public static Bounds getScenePos(Node node) {
        return node.localToScene(node.getBoundsInLocal());
    }

    public static Bounds getScreenPos(Node node) {
        return node.localToScreen(node.getBoundsInLocal());
    }

    public static double getSceneMaxX(Scene scene) {
        return scene.getX() + scene.getWidth();
    }

    public static double getSceneMaxY(Scene scene) {
        return scene.getX() + scene.getHeight();
    }

    public static Object getLastElement(List<?> list) {
        return list.get(list.size() - 1);
    }

    /**
     * Removes a subset from the list of persons.
     * @param persons The list of persons
     * @param personsToRemove The subset of persons.
     * @return The modified persons after removal of the subset from persons.
     */
    public static TestTask[] removePersonsFromList(final TestTask[] persons, TestTask... personsToRemove) {
        List<TestTask> listOfPersons = asList(persons);
        listOfPersons.removeAll(asList(personsToRemove));
        return listOfPersons.toArray(new TestTask[listOfPersons.size()]);
    }

    /**
     * Returns a copy of the list with the person at specified index removed.
     * @param list original list to copy from
     * @param targetIndexInOneIndexedFormat e.g. if the first element to be removed, 1 should be given as index.
     */
    public static TestTask[] removePersonFromList(final TestTask[] list, int targetIndexInOneIndexedFormat) {
        return removePersonsFromList(list, list[targetIndexInOneIndexedFormat-1]);
    }

    /**
     * @@author A0138993L
     * Removes a subset from the list of deadliness.
     * @param deadlines The list of deadliness
     * @param deadlinesToRemove The subset of deadlines.
     * @return The modified deadlines after removal of the subset from deadlines.
     */
    public static TestDeadline[] removeDeadlinesFromList(final TestDeadline[] persons, TestDeadline... personsToRemove) {
        List<TestDeadline> listOfPersons = asList(persons);
        listOfPersons.removeAll(asList(personsToRemove));
        return listOfPersons.toArray(new TestDeadline[listOfPersons.size()]);
    }
    
    /**
     * @@author A0138993L
     * Removes a subset from the list of todo.
     * @param todos The list of todos
     * @param todosToRemove The subset of todos.
     * @return The modified todos after removal of the subset from todos.
     */
    public static TestTodo[] removeTodosFromList(final TestTodo[] persons, TestTodo... personsToRemove) {
        List<TestTodo> listOfPersons = asList(persons);
        listOfPersons.removeAll(asList(personsToRemove));
        return listOfPersons.toArray(new TestTodo[listOfPersons.size()]);
    }

    /**
     * @@author A0138993L
     * Returns a copy of the list with the deadline at specified index removed.
     * @param list original list to copy from
     * @param targetIndexInOneIndexedFormat e.g. if the first element to be removed, 1 should be given as index.
     */
    public static TestDeadline[] removeDeadlinesFromList(final TestDeadline[] list, int targetIndexInOneIndexedFormat) {
        return removeDeadlinesFromList(list, list[targetIndexInOneIndexedFormat-1]);
    }

    /**
     * @@author A0138993L
     * Returns a copy of the list with the todo at specified index removed.
     * @param list original list to copy from
     * @param targetIndexInOneIndexedFormat e.g. if the first element to be removed, 1 should be given as index.
     */
    public static TestTodo[] removeTodosFromList(final TestTodo[] list, int targetIndexInOneIndexedFormat) {
        return removeTodosFromList(list, list[targetIndexInOneIndexedFormat-1]);
    }
    
    /**
     * Replaces persons[i] with a person.
     * @param persons The array of persons.
     * @param person The replacement person
     * @param index The index of the person to be replaced.
     * @return
     */
    public static TestTask[] replacePersonFromList(TestTask[] persons, TestTask person, int index) {
        persons[index] = person;
        return persons;
    }

    /**
     * Appends persons to the array of persons.
     * @param persons A array of persons.
     * @param personsToAdd The persons that are to be appended behind the original array.
     * @return The modified array of persons.
     */
    public static TestTask[] addPersonsToList(final TestTask[] persons, TestTask... personsToAdd) {
        List<TestTask> listOfPersons = asList(persons);
        listOfPersons.addAll(asList(personsToAdd));
        return listOfPersons.toArray(new TestTask[listOfPersons.size()]);
    }
    
    /**
     * @@author A0138993L
     * Appends deadlines to the array of deadlines.
     * @param deadlines A array of deadlines.
     * @param deadlinesToAdd The deadlines that are to be appended behind the original array.
     * @return The modified array of deadlines.
     */
    public static TestDeadline[] addDeadlinesToList(final TestDeadline[] persons, TestDeadline... personsToAdd) {
        List<TestDeadline> listOfPersons = asList(persons);
        listOfPersons.addAll(asList(personsToAdd));
        return listOfPersons.toArray(new TestDeadline[listOfPersons.size()]);
    }
    
    /**
     * @@author A0138993L
     * Appends todos to the array of todos.
     * @param todos A array of todos.
     * @param todosToAdd The todos that are to be appended behind the original array.
     * @return The modified array of todos.
     */
    public static TestTodo[] addTodosToList(final TestTodo[] persons, TestTodo... personsToAdd) {
        List<TestTodo> listOfPersons = asList(persons);
        listOfPersons.addAll(asList(personsToAdd));
        return listOfPersons.toArray(new TestTodo[listOfPersons.size()]);
    }

    private static <T> List<T> asList(T[] objs) {
        List<T> list = new ArrayList<>();
        for(T obj : objs) {
            list.add(obj);
        }
        return list;
    }

    public static boolean compareCardAndPerson(PersonCardHandle card, ReadOnlyTask person) {
        return card.isSamePerson(person);
    }
    //@@author A0138993L
    public static boolean compareCardAndDeadline(DeadlineCardHandle card, ReadOnlyTask person) {
    	//System.out.println(card + " " + person + "compareCardAndDeadline");
        return card.isSamePerson(person);
    }
  //@@author A0138993L
    public static boolean compareCardAndTodo(TodoCardHandle card, ReadOnlyTask person) {
        return card.isSamePerson(person);
    }

    public static Tag[] getTagList(String tags) {

        if (tags.equals("")) {
            return new Tag[]{};
        }

        final String[] split = tags.split(", ");

        final List<Tag> collect = Arrays.asList(split).stream().map(e -> {
            try {
                return new Tag(e.replaceFirst("Tag: ", ""));
            } catch (IllegalValueException e1) {
                //not possible
                assert false;
                return null;
            }
        }).collect(Collectors.toList());

        return collect.toArray(new Tag[split.length]);
    }

}
