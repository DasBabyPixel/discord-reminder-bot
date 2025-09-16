import java.time.Duration;

public class Reminder {
    private final String name;
    private final Duration offsetBeforeEvent;
    private final long channelId;
    private String message;

    public Reminder(String name, Duration offsetBeforeEvent, long channelId, String message) {
        this.name = name;
        this.offsetBeforeEvent = offsetBeforeEvent;
        this.channelId = channelId;
        this.message = message;
    }

    public String message() {
        return message;
    }

    public String name() {
        return name;
    }

    public Duration offsetBeforeEvent() {
        return offsetBeforeEvent;
    }

    public long channelId() {
        return channelId;
    }
}
