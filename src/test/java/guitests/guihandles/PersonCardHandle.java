package guitests.guihandles;

import guitests.GuiRobot;
import javafx.scene.Node;
import javafx.stage.Stage;
import seedu.simply.model.task.ReadOnlyTask;

/**
 * Provides a handle to a person card in the person list panel.
 */
public class PersonCardHandle extends GuiHandle {
    private static final String NAME_FIELD_ID = "#name";
    private static final String END_FIELD_ID = "#end";
    private static final String DATE_FIELD_ID = "#date";
    private static final String START_FIELD_ID = "#start";

    private Node node;

    public PersonCardHandle(GuiRobot guiRobot, Stage primaryStage, Node node){
        super(guiRobot, primaryStage, null);
        this.node = node;
    }

    protected String getTextFromLabel(String fieldId) {
        return getTextFromLabel(fieldId, node);
    }

    public String getFullName() {
        return getTextFromLabel(NAME_FIELD_ID);
    }

    public String getTask() {
        return getTextFromLabel(END_FIELD_ID);
    }

    public String getPhone() {
        return getTextFromLabel(DATE_FIELD_ID);
    }

    public String getEmail() {
        return getTextFromLabel(START_FIELD_ID);
    }

    public boolean isSamePerson(ReadOnlyTask person){
        return getFullName().equals(person.getName().toString()) && getPhone().substring(5).trim().equals(person.getDate().toString());
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof PersonCardHandle) {
            PersonCardHandle handle = (PersonCardHandle) obj;
            return getFullName().equals(handle.getFullName())
                    && getTask().equals(handle.getTask())
                    && getPhone().equals(handle.getPhone())
                    && getEmail().equals(handle.getEmail()); 
        }
        return super.equals(obj);
    }

    @Override
    public String toString() {
        return getFullName() + " " + getTask();
    }
}
