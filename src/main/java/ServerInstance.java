import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.MessageCreateSpec;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class ServerInstance {
    private static final Logger LOGGER = Loggers.getLogger(ServerInstance.class);
    private final long id;
    private final Map<String, Event> events = new HashMap<>();
    private final Map<String, ReactionRolesMessage> reactionRoles = new HashMap<>();
    private final Timer timer;
    private final Map<Reminder, TimerTask> timers = new HashMap<>();
    private final Map<Reminder, TimerTask> offsetTimers = new HashMap<>();

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
            for (var event : eventMap.values()) {
                startTimers(event);
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

        public void addReactionRolesMessage(String msgId, long channelId, long messageId, String content) throws IOException {
            if (reactionRoles().containsKey(msgId)) throw new IllegalStateException();
            var rr = new ReactionRolesMessage(msgId, channelId, messageId, content);
            reactionRoles().put(msgId, rr);
            save();
        }

        public boolean removeReactionRolesMessage(String msgId) throws IOException {
            var suc = reactionRoles().remove(msgId) != null;
            if (suc) save();
            return suc;
        }

        public void addReactionRolesRole(String msgId, long roleId, String roleDisplay) throws IOException {
            var rr = Objects.requireNonNull(reactionRoles().get(msgId));
            if (rr.getRoles().containsKey(roleId)) throw new IllegalStateException();
            rr.getRoles().put(roleId, new ReactionRolesMessage.RoleEntry(roleId, roleDisplay));
            save();
        }

        public void updateRoleDisplay(String msgId, long roleId, String roleDisplay) throws IOException {
            var rr = Objects.requireNonNull(reactionRoles().get(msgId));
            if (!rr.getRoles().containsKey(roleId)) throw new IllegalStateException("Role does not exist");
            rr.getRoles().put(roleId, new ReactionRolesMessage.RoleEntry(roleId, roleDisplay));
            save();
        }

        public void removeReactionRolesRole(String msgId, long roleId) throws IOException {
            var rr = Objects.requireNonNull(reactionRoles().get(msgId));
            if (!rr.getRoles().containsKey(roleId)) throw new IllegalStateException();
            rr.getRoles().remove(roleId);
            save();
        }

        private void start(Reminder rem, Instant start, Duration interval, boolean isOffset) {
            var task = new TimerTask() {
                @Override
                public void run() {
                    LOGGER.info("Sending reminder for " + rem.name());
                    ReminderBot.gateway
                            .getChannelById(Snowflake.of(rem.channelId()))
                            .ofType(MessageChannel.class)
                            .flatMap(channel -> channel.createMessage(MessageCreateSpec
                                    .builder().content(rem.message()).build()))
                            .doOnError(throwable -> LOGGER.error("Failed to send message", throwable))
                            .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
                            .subscribe();
                    if (isOffset) {
                        LOGGER.info("Cancelling single-offset reminder");
                        cancel();
                        synchronized (ServerInstance.this) {
                            offsetTimers.remove(rem, this);
                        }
                    }
                }
            };
            timer.scheduleAtFixedRate(task, Date.from(start), interval.toMillis());
            LOGGER.info("Starting timer for reminder " + rem.name() + " with first execution " + start + " offset:" + isOffset);
            if (isOffset) {
                offsetTimers.put(rem, task);
            } else {
                timers.put(rem, task);
            }
        }

        public boolean moveEventTo(String eventName, Instant firstTime) throws IOException {
            var event = events.get(eventName);
            if (event == null) return false;
            event.moveTo(firstTime);
            restartTimers(event);
            save();
            return true;
        }

        public boolean moveEventBy(String eventName, Duration duration) throws IOException {
            var event = events.get(eventName);
            if (event == null) return false;
            event.moveBy(duration);
            restartTimers(event);
            save();
            return true;
        }

        public boolean moveEventByOnce(String eventName, Duration duration) throws IOException {
            var event = events.get(eventName);
            if (event == null) return false;
            event.moveByOnce(duration);
            restartTimers(event);
            save();
            return true;
        }

        private void restartTimers(Event event) {
            stopTimers(event);
            startTimers(event);
        }

        private void stopTimers(Event event) {
            LOGGER.info("Stopping all timers for event " + event.name());
            for (var rem : event.reminders().values()) {
                timers.remove(rem).cancel();
            }
            for (var rem : event.reminders().values()) {
                var t = offsetTimers.remove(rem);
                if (t != null) t.cancel();
            }
            LOGGER.info("Stopped all timers for event " + event.name());
        }

        private void startTimers(Event event) {
            LOGGER.info("Starting all timers for event " + event.name());
            var now = Instant.now();
            for (var rem : event.reminders().values()) {
                if (event.isNextOffset(now)) {
                    LOGGER.info("Detected offset: " + event.getOffsetNextTime());

                    var nextNormalStart = Event
                            .getNextExecution(event.firstTime(), now, event.interval())
                            .plus(event.interval())
                            .minus(rem.offsetBeforeEvent());
                    start(rem, nextNormalStart, event.interval(), false);

                    var offsetStart = event.getNextReminderTime(now, rem.offsetBeforeEvent());
                    if (now.isBefore(offsetStart)) {
                        start(rem, offsetStart, event.interval(), true);
                    } else {
                        LOGGER.info("Skipping offset once");
                    }

                } else {
                    start(rem, event.getNextReminderTime(now, rem.offsetBeforeEvent()), event.interval(), false);
                }
            }
            LOGGER.info("Started all timers for event " + event.name());
        }

        public void addReminder(String eventName, String reminderName, Duration offset, String message, long channelId) throws IOException {
            var event = events.get(eventName);
            if (event == null) throw new IllegalArgumentException("Unknown event: " + eventName);
            var rem = new Reminder(reminderName, offset, channelId, message);
            stopTimers(event);
            event.addReminder(rem);
            save();
            startTimers(event);
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
            LOGGER.info("Saving configuration for " + this.id());
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
