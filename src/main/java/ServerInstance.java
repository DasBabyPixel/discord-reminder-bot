import java.util.HashMap;
import java.util.Map;

public class ServerInstance {
    private final long id;
    private final Map<String, Reminder> reminders = new HashMap<>();

    public ServerInstance(long id) {
        this.id = id;
    }

    public boolean addReminder(Reminder reminder) {
        if (reminders.containsKey(reminder.name())) return false;
        reminders.put(reminder.name(), reminder);
        return true;
    }

    public long id() {
        return id;
    }

    public Map<String, Reminder> reminders() {
        return reminders;
    }
}
