import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class Event {
    private final String name;
    private Instant firstTime;
    private final Duration interval;
    private final Map<String, Reminder> reminders;

    public Event(String name, Instant firstTime, Duration interval, Map<String, Reminder> reminders) {
        this.name = name;
        this.firstTime = firstTime;
        this.interval = interval;
        this.reminders = new HashMap<>(reminders);
    }

    public Instant getNextReminderTime(Instant now, Duration offset) {
        return getNextExecution(firstTime.minus(offset), now, interval);
    }

    public Instant getNextEventTime(Instant now) {
        return getNextExecution(firstTime, now, interval);
    }

    public static Instant getNextExecution(Instant firstTime, Instant now, Duration interval) {
        var difference = firstTime.until(now);
        var countSinceBegin = difference.dividedBy(interval);
        if (now.compareTo(firstTime) > 0) countSinceBegin++;
        return firstTime.plus(interval.multipliedBy(countSinceBegin));
    }

    public String name() {
        return name;
    }

    /**
     * Moves the events start point by some duration
     */
    public void moveBy(Duration duration) {
        firstTime = firstTime.plus(duration);
    }

    public Instant firstTime() {
        return firstTime;
    }

    public Duration interval() {
        return interval;
    }

    public Map<String, Reminder> reminders() {
        return reminders;
    }

    public void eventCalled() {
    }

    public Reminder removeReminder(String name) {
        return reminders.remove(name);
    }

    public void addReminder(Reminder reminder) {
        if (reminders.containsKey(reminder.name())) {
            throw new IllegalArgumentException("Reminder already registered with name " + reminder.name());
        }
        reminders.put(reminder.name(), reminder);
    }
}
