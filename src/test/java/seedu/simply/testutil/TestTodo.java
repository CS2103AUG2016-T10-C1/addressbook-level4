package seedu.simply.testutil;

import seedu.simply.model.tag.UniqueTagList;
import seedu.simply.model.task.*;

/**
 * @@author A0138993L
 * A mutable person object. For testing only.
 */
public class TestTodo implements ReadOnlyTask {

    private Name name;
    private End end;
    private Start start;
    private Date date;
    private int task_cat;
    private int overdue;
    private UniqueTagList tags;
    boolean isCompleted;
    
    public TestTodo() {
        tags = new UniqueTagList();
    }

    public void setName(Name name) {
        this.name = name;
    }

    public void setEnd(End task) {
        this.end = task;
    }

    public void setStart(Start email) {
        this.start = email;
    }

    public void setDate(Date phone) {
        this.date = phone;
    }
    
    public void setTaskCategory(int taskCat) {
    	this.task_cat = taskCat;
    }
    
    public void setOverdue(int overdue) {
    	this.overdue = overdue;
    }
    
    public void setIsCompleted(boolean isCompleted) {
    	this.isCompleted = isCompleted;
    }

    @Override
    public Name getName() {
        return name;
    }

    @Override
    public Date getDate() {
        return date;
    }

    @Override
    public Start getStart() {
        return start;
    }

    @Override
    public End getEnd() {
        return end;
    }
    
    @Override
    public int getTaskCategory() {
    	return task_cat;
    }
    
    @Override
    public int getOverdue(){
    	return overdue;
    }
    
    @Override
    public boolean getIsCompleted() {
    	return isCompleted;
    }

    @Override
    public UniqueTagList getTags() {
        return tags;
    }

    @Override
    public String toString() {
        return getAsText();
    }

    public String getAddCommand() {
        StringBuilder sb = new StringBuilder();
        sb.append("add " + this.getName().taskDetails + " ");
        this.getTags().getInternalList().stream().forEach(s -> sb.append("#" + s.tagName + " "));
        return sb.toString();
    }
}
