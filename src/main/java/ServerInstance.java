import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.MessageCreateSpec;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class ServerInstance {
    private final long id;
    private final Map<String, Event> events = new HashMap<>();
    private final Map<String, ReactionRolesMessage> reactionRoles = new HashMap<>();
    private final Timer timer;
    private final Map<Reminder, TimerTask> timers = new HashMap<>();

    public ServerInstance(long id) {
        this.id = id;
        this.timer = new Timer("Server-" + id);
    }

    public <T, E extends Throwable> T useFun(Fun<Usable, T, E> function) throws E {
        synchronized (this) {
            var u = new Usable();
            try {
                return function.apply(u);
            } finally {
                u.valid = false;
            }
        }
    }

    public <E extends Throwable> void useCal(Cal<Usable, E> function) throws E {
        synchronized (this) {
            var u = new Usable();
            try {
                function.apply(u);
            } finally {
                u.valid = false;
            }
        }
    }

    public interface Cal<I, T extends Throwable> {
        void apply(I input) throws T;
    }

    public interface Fun<I, O, T extends Throwable> {
        O apply(I input) throws T;
    }

    public class Usable {
        private volatile boolean valid = true;

        public long id() {
            return id;
        }

        void addAllEvents(Map<String, Event> eventMap) {
            events.putAll(eventMap);
            var now = Instant.now();
            for (var event : eventMap.values()) {
                for (var reminder : event.reminders().values()) {
                    start(reminder, event.getNextReminderTime(now, reminder.offsetBeforeEvent()), event.interval());
                }
            }
        }

        public Map<String, Event> events() {
            if (!valid) throw new UnsupportedOperationException();
            return events;
        }

        public Map<String, ReactionRolesMessage> reactionRoles() {
            if (!valid) throw new UnsupportedOperationException();
            return reactionRoles;
        }

        public long updateReactionRolesMessage(String msgId, long channelId, String content) throws IOException {
            var rr = Objects.requireNonNull(reactionRoles().get(msgId));
            if (rr.getChannelId() != channelId)
                throw new IllegalArgumentException("A message with that ID already exists in a different channel");
            rr.content(content);
            save();
            return rr.getMessageId();
        }

        public boolean addReactionRolesMessage(String msgId, long channelId, long messageId, String content) throws IOException {
            if (reactionRoles().containsKey(msgId)) return false;
            var rr = new ReactionRolesMessage(msgId, channelId, messageId, content);
            reactionRoles().put(msgId, rr);
            save();
            return true;
        }

        public boolean removeReactionRolesMessage(String msgId) throws IOException {
            var suc = reactionRoles().remove(msgId) != null;
            if (suc) save();
            return suc;
        }

        public boolean addReactionRolesRole(String msgId, long roleId, String roleDisplay) throws IOException {
            var rr = Objects.requireNonNull(reactionRoles().get(msgId));
            if (rr.getRoles().containsKey(roleId)) return false;
            rr.getRoles().put(roleId, new ReactionRolesMessage.RoleEntry(roleId, roleDisplay));
            save();
            return true;
        }

        public void updateRoleDisplay(String msgId, long roleId, String roleDisplay) throws IOException {
            var rr = Objects.requireNonNull(reactionRoles().get(msgId));
            if (!rr.getRoles().containsKey(roleId)) throw new IllegalStateException("Role does not exist");
            rr.getRoles().put(roleId, new ReactionRolesMessage.RoleEntry(roleId, roleDisplay));
            save();
        }

        public boolean removeReactionRolesRole(String msgId, long roleId) throws IOException {
            var rr = Objects.requireNonNull(reactionRoles().get(msgId));
            if (!rr.getRoles().containsKey(roleId)) return false;
            rr.getRoles().remove(roleId);
            save();
            return true;
        }

        private void start(Reminder rem, Instant start, Duration interval) {
            var task = new TimerTask() {
                @Override
                public void run() {
                    ReminderBot.gateway
                            .getChannelById(Snowflake.of(rem.channelId()))
                            .ofType(MessageChannel.class)
                            .flatMap(channel -> channel.createMessage(MessageCreateSpec
                                    .builder()
                                    .content(rem.message())
                                    .build()))
                            .subscribe();
                }
            };
            timer.scheduleAtFixedRate(task, Date.from(start), interval.toMillis());
            timers.put(rem, task);
        }

        public void addReminder(String eventName, String reminderName, Duration offset, String message, long channelId) throws IOException {
            var event = events.get(eventName);
            if (event == null) throw new IllegalArgumentException("Unknown event: " + eventName);
            var rem = new Reminder(reminderName, offset, channelId, message);
            event.addReminder(rem);
            save();
            start(rem, event.getNextReminderTime(Instant.now(), rem.offsetBeforeEvent()), event.interval());
        }

        public boolean removeReminder(String eventName, String reminderName) throws IOException {
            var event = events.get(eventName);
            if (event == null) throw new IllegalArgumentException("Unknown event: " + eventName);
            var s = event.removeReminder(reminderName);
            if (s != null) {
                timers.remove(s).cancel();
                save();
            }
            return s != null;
        }

        public void save() throws IOException {
            ServerManager.INSTANCE.save(this);
        }

        public boolean removeEvent(String name) throws IOException {
            var e = events.remove(name);
            if (e == null) return false;
            for (var value : e.reminders().values()) {
                timers.remove(value).cancel();
            }
            save();
            return true;
        }

        public boolean addEvent(String name, Instant firstTime, Duration interval) throws IOException, UnsupportedOperationException {
            if (!valid) throw new UnsupportedOperationException();
            if (events.containsKey(name)) return false;
            var event = new Event(name, firstTime, interval, Map.of());
            events.put(name, event);
            save();
            return true;
        }
    }
}
