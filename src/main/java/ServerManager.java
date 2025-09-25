import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerManager {
    public static final ServerManager INSTANCE = new ServerManager();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<Long, ServerInstance> servers = new ConcurrentHashMap<>();
    private final Path serversPath;

    private ServerManager() {
        serversPath = Path.of("servers");
        init();
    }

    public static void load() {
    }

    private void init() {
        try (var list = Files.list(serversPath)) {
            list.forEach(serverPath -> {
                try (var reader = Files.newBufferedReader(serverPath)) {
                    var json = gson.fromJson(reader, JsonObject.class);
                    var events = json.get("events").getAsJsonObject();
                    var eventMap = new HashMap<String, Event>();
                    for (var eventName : events.keySet()) {
                        var eventJson = events.get(eventName).getAsJsonObject();
                        var firstTime = Instant.ofEpochSecond(eventJson.get("firstTime").getAsLong());
                        var interval = Duration.ofSeconds(eventJson.get("interval").getAsLong());

                        var reminderMap = new HashMap<String, Reminder>();

                        var reminders = eventJson.get("reminders").getAsJsonObject();
                        for (var reminderName : reminders.keySet()) {
                            var reminderJson = reminders.get(reminderName).getAsJsonObject();
                            var channelId = reminderJson.get("channelId").getAsLong();
                            var message = reminderJson.get("message").getAsString();
                            var offset = Duration.ofSeconds(reminderJson.get("offset").getAsLong());

                            var reminder = new Reminder(reminderName, offset, channelId, message);
                            reminderMap.put(reminderName, reminder);
                        }

//                        var manual = new HashMap<Integer, Duration>();
//                        var manualJson = eventJson.get("manual").getAsJsonObject();
//                        for (var manualKey : manualJson.keySet()) {
//                            manual.put(Integer.parseInt(manualKey), Duration.ofSeconds(manualJson
//                                    .get(manualKey)
//                                    .getAsLong()));
//                        }

                        var event = new Event(eventName, firstTime, interval, reminderMap);
                        eventMap.put(eventName, event);
                    }
                    var reactionRoles = new HashMap<String, ReactionRolesMessage>();
                    if (json.has("reactionRoles")) {
                        var reactionRolesJson = json.get("reactionRoles").getAsJsonObject();
                        for (var msgId : reactionRolesJson.keySet()) {
                            var j = reactionRolesJson.get(msgId).getAsJsonObject();
                            var messageId = j.get("messageId").getAsLong();
                            var channelId = j.get("channelId").getAsLong();
                            var content = j.get("content").getAsString();
                            var roles = new HashMap<Long, ReactionRolesMessage.RoleEntry>();
                            var rolesJson = j.get("roles").getAsJsonObject();

                            for (var roleIdS : rolesJson.keySet()) {
                                var roleId = Long.parseLong(roleIdS);
                                var roleDisplay = rolesJson.get(roleIdS).getAsString();
                                roles.put(roleId, new ReactionRolesMessage.RoleEntry(roleId, roleDisplay));
                            }

                            var msg = new ReactionRolesMessage(msgId, channelId, messageId, content);
                            msg.getRoles().putAll(roles);
                            reactionRoles.put(msgId, msg);
                        }
                    }

                    get(Long.parseLong(serverPath.getFileName().toString())).useCal(input -> {
                        input.addAllEvents(eventMap);
                        input.reactionRoles().putAll(reactionRoles);
                    });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ServerInstance get(long id) {
        return servers.computeIfAbsent(id, ServerInstance::new);
    }

    public void save(ServerInstance.Usable instance) throws IOException {
        var path = serversPath.resolve(Long.toString(instance.id()));
        Files.createDirectories(serversPath);
        try (var writer = Files.newBufferedWriter(path)) {
            var json = new JsonObject();
            var events = new JsonObject();
            for (var event : instance.events().values()) {
                var ej = new JsonObject();
                ej.addProperty("firstTime", event.firstTime().getEpochSecond());
                ej.addProperty("interval", event.interval().toSeconds());
                var rems = new JsonObject();
                for (var reminder : event.reminders().values()) {
                    var rj = new JsonObject();
                    rj.addProperty("channelId", reminder.channelId());
                    rj.addProperty("message", reminder.message());
                    rj.addProperty("offset", reminder.offsetBeforeEvent().toSeconds());

//                    var manual = new JsonObject();
//                    for (var e : reminder.manualOffsets().entrySet()) {
//                        manual.addProperty(e.getKey().toString(), e.getValue().toSeconds());
//                    }
//                    rj.add("manual", manual);

                    rems.add(reminder.name(), rj);
                }
                ej.add("reminders", rems);

                events.add(event.name(), ej);
            }
            json.add("events", events);
            var reactionRoles = new JsonObject();
            for (var message : instance.reactionRoles().values()) {
                var roles = new JsonObject();
                for (var role : message.getRoles().values()) {
                    roles.addProperty(Long.toString(role.roleId()), role.display());
                }
                var j = new JsonObject();
                j.add("roles", roles);
                j.addProperty("messageId", message.getMessageId());
                j.addProperty("channelId", message.getChannelId());
                j.addProperty("content", message.getContent());
                reactionRoles.add(message.getMsgId(), j);
            }
            json.add("reactionRoles", reactionRoles);

            gson.toJson(json, writer);
        }
    }
}
