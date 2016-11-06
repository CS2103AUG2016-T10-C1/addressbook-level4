package seedu.simply.model.task;


import java.time.LocalTime;

import seedu.simply.commons.exceptions.IllegalValueException;

/**
 * @@author A0138993L
 * Represents a task's end time in Simply
 * Guarantees: immutable; is valid as declared in {@link #isValidEnd(String)}
 */
public class End implements Comparable<End> {
    
    public static final String MESSAGE_END_CONSTRAINTS = "Task end time can be entered in 24hour or 12hour format.";
    public static final String END_VALIDATION_REGEX = "([01]\\d{1}[0-5]\\d{1})|"
            + "([2][0-3][0-5]\\d{1})|"
            + "([1-9](?:pm|am|PM|AM))|" 
            + "(1[0-2](?:pm|am|PM|AM))|"
            + "([1-9]\\.[0-5]{1}\\d{1}(?:pm|am))|"
            + "(1[0-2]\\.[0-5]{1}\\d{1}(?:pm|am))|"
            + "(no end)";
    public static final String DEFAULT_END_TIME = "2359";
    public final String value;
    private int pastEndTime =0;

    /**
     *
     * Validates given end time.
     * @@author A0138993L
     * @throws IllegalValueException if given task string is invalid.
     */
    public End(String end) throws IllegalValueException {
        if (end == null) {
            end = "default";
        }
        if (!isValidEnd(end)) {
            throw new IllegalValueException(MESSAGE_END_CONSTRAINTS);
        }
        if (end.equals("default")) {
            this.value = DEFAULT_END_TIME;
        } else if (end.equals("no end")) {
            this.value = "no end";
        } else {
            this.value = changeTo24HourFormat(end);
            if (isPastEndTime(value)) {
                pastEndTime =1;
            }
        }
    }
    /**
     * @@author A0138993L
     * checks if the end time have past
     * @param end the user input end time
     * @return true is it has past and false if it has not past
     */
    public boolean isPastEndTime(String end) {
    	String localTime = new String("");
    	String new_min = formatLocalTimeMinutes();
        String new_hr = formatLocalTimeHours();
		localTime = new_hr +""+ new_min;
		if (Integer.parseInt(end) - Integer.parseInt(localTime) < 0) {
			return true;
		} else {
			return false;
		}
	}
    /**
     * @@author A0138993L
     * formatting the local time class hours to the desired format
     * @return the formatted hours
     */
    private String formatLocalTimeHours() {
        String new_hr = new String(LocalTime.now().getHour() + "");
		if (new_hr.length() ==1) {
			new_hr = "0" + new_hr;
		}
        return new_hr;
    }
    /**
     * @@author A0138993L
     * formatting the local time class minutes to the desired format
     * @return the formatted minutes
     */
    private String formatLocalTimeMinutes() {
        String new_min = new String(LocalTime.now().getMinute() + "");
    	if (new_min.length() ==1 ) {
            new_min = "0" + new_min;
    	}
        return new_min;
    }
    /**
     * @@author A0138993L
     * changing the end time to 24 hour format for easier sorting and display
     * @param end the user input end time
     * @return the standardize format of the user end time
     */
    private String changeTo24HourFormat(String end) {
        if (Character.isDigit(end.charAt(end.length()-1))) {
            return end;
        } else if (end.length() == 3) {
            if (end.substring(1).equalsIgnoreCase("pm")) {
                return (Integer.parseInt(end.substring(0,1))+12) + "00";
            } else {
                return "0" + end.substring(0, 1) + "00";
            }
        } else if (end.length() == 4) {
            if (end.substring(2).equalsIgnoreCase("pm")) {
                return (Integer.parseInt(end.substring(0,2))+12) + "00";
            } else {
                return end.substring(0, 2) + "00";
            }
        } else {
            String[] time_cat = end.split("\\.");
            if (time_cat[0].length() ==1) {
                time_cat[0] = "0" + time_cat[0];
            }
            if (time_cat[1].substring(2).equalsIgnoreCase("pm")) {
                time_cat[0] = "" + (Integer.parseInt(time_cat[0]) + 12);
            }
            return time_cat[0] + time_cat[1].substring(0, 2);
        }

    }

	public int getPastEndTime() {
		return pastEndTime;
	}

    /**
     * Returns true if a given string is a valid task end time.
     */
	public static boolean isValidEnd(String test) {
	    if (test.matches(END_VALIDATION_REGEX) || test.equals("default")) {
	        return true;
	    } else {
	        return false;
	    }
	}

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other == this // short circuit if same object
                || (other instanceof End // instanceof handles nulls
                && this.value.equals(((End) other).value)); // state check
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
    
  //@@author A0139430L
    @Override
    public int compareTo(End o) {
        if(this.value.compareTo("no end") == 0 & o.toString().compareTo("no end") == 0) {
            return 0;
        } else if(this.value.compareTo("no end") == 0 ) {
            return -1;
        } else if(o.toString().compareTo("no end") == 0 ) {
            return 1;
        }
        return this.value.compareTo(o.toString());
    }

}