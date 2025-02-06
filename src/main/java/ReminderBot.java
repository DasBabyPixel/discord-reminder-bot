import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ReminderBot {
    public static GatewayDiscordClient gateway;
    private static final Map<String, Reminder> REMINDERS = new HashMap<>();

    public static void main(String[] args) {
        login(args[0]);

        ConsoleReader.start();

        var guildId = 1331253401643647077L;
//        var remindersChannelId = 1337131762886901820L;
        var userBabyPixelId = 395655395114483713L;
        var channelBear1Id = 1337131762886901820L;
        var roleBear1Id = 1337128132481650758L;
        var channelBear2Id = 1337163055242416221L;
        var roleBear2Id = 1337128233077837976L;
        var channelResetId = 1337163171038756966L;
        var roleResetId = 1337145958160662558L;

        var guild = Objects.requireNonNull(gateway.getGuildById(Snowflake.of(guildId)).block());
        var remindersBear1Channel = gateway
                .getChannelById(Snowflake.of(channelBear1Id))
                .ofType(MessageChannel.class)
                .block();
        var remindersBear2Channel = gateway
                .getChannelById(Snowflake.of(channelBear2Id))
                .ofType(MessageChannel.class)
                .block();
        var remindersResetChannel = gateway
                .getChannelById(Snowflake.of(channelResetId))
                .ofType(MessageChannel.class)
                .block();
        var roleBear1 = gateway.getRoleById(guild.getId(), Snowflake.of(roleBear1Id)).block();
        var roleBear2 = gateway.getRoleById(guild.getId(), Snowflake.of(roleBear2Id)).block();
        var roleReset = gateway.getRoleById(guild.getId(), Snowflake.of(roleResetId)).block();

        gateway.on(ButtonInteractionEvent.class, event -> {
            var id = event.getCustomId();
            Role role;
            String display;
            switch (id) {
                case "bear1" -> {
                    role = roleBear1;
                    display = "Bear 1";
                }
                case "bear2" -> {
                    role = roleBear2;
                    display = "Bear 2";
                }
                case "reset" -> {
                    role = roleReset;
                    display = "Reset";
                }
                default -> {
                    role = roleBear1;
                    display = "INVALID";
                }
            }
            var member = event.getInteraction().getMember().orElseThrow();
            return member.getRoles().collectList().flatMap(roles -> {
                var has = roles.stream().map(s -> s.getId().asLong()).anyMatch(i -> role.getId().asLong() == i);
                if (has) {
                    return member
                            .removeRole(role.getId())
                            .then(Mono.defer(() -> event.reply(InteractionApplicationCommandCallbackSpec
                                    .builder()
                                    .ephemeral(true)
                                    .content("Reminders for " + display + " disabled.")
                                    .build())));
                } else {
                    return member
                            .addRole(role.getId())
                            .then(Mono.defer(() -> event.reply(InteractionApplicationCommandCallbackSpec
                                    .builder()
                                    .ephemeral(true)
                                    .content("Reminders for " + display + " enabled.")
                                    .build())));
                }
            });
        }).subscribe();

        var timer = new Timer("Bear-Timer", true);

        var bear1Start = 1738868100000L;
        var bear2Start = 1738887000000L;
        var resetTime = 1738886100000L;

        startBearTimer(timer, bear1Start, remindersBear1Channel, roleBear1);
        startBearTimer(timer, bear2Start, remindersBear2Channel, roleBear2);
        startResetTimer(timer, resetTime, remindersResetChannel, roleReset);
    }

    private static void startResetTimer(Timer timer, long originalStartTime, MessageChannel channel, Role role) {
        var interval = TimeUnit.DAYS.toMillis(1);
        var startTime = adjustStart(originalStartTime, interval);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                scheduleNotification(channel, "Reset alarm <@&%d>".formatted(role.getId().asLong()));
            }
        }, Date.from(Instant.ofEpochMilli(startTime)), interval);
    }

    private static void startBearTimer(Timer timer, long originalStartTime, MessageChannel channel, Role role) {
        var interval = TimeUnit.DAYS.toMillis(2);
        var startTime = adjustStart(originalStartTime, interval);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                scheduleNotification(channel, "Bear alarm <@&%d>".formatted(role.getId().asLong()));
            }
        }, Date.from(Instant.ofEpochMilli(startTime)), interval);
    }

    private static long adjustStart(long origin, long interval) {
        var currentTime = System.currentTimeMillis();
        var difference = currentTime - origin;
        var sinceBegin = difference / interval;
        if (currentTime > origin) sinceBegin++;
        return origin + sinceBegin * interval;
    }

    private static void scheduleNotification(MessageChannel channel, String content) {
        channel.createMessage(MessageCreateSpec.builder().content(content).build()).subscribe();
    }

    public static synchronized Reminder getReminder(String name) {
        return REMINDERS.get(name);
    }

    public static synchronized boolean addReminder(Reminder reminder) {
        if (REMINDERS.containsKey(reminder.name())) return false;
        REMINDERS.put(reminder.name(), reminder);
        return true;
    }

    public static synchronized void removeReminder(String name) {
        REMINDERS.remove(name);
    }

    public static synchronized List<Reminder> getReminders() {
        return List.copyOf(REMINDERS.values());
    }

    public static GatewayDiscordClient login(String token) {
        System.out.println("Working in " + Path.of("t").toAbsolutePath().getParent());
        var client = DiscordClient.create(token);
        gateway = Objects.requireNonNull(client.login().block());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> gateway.logout().block()));
        return gateway;
    }
}
