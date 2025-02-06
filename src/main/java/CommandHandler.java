import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;

import java.util.stream.Collectors;

public class CommandHandler {
    public static void register(GatewayDiscordClient gateway) {
        gateway.on(ChatInputInteractionEvent.class, event -> {
            var command = event.getCommandName();
            switch (command) {
                case "serverreminder" -> {
                    var createOption = event.getOption("create").orElse(null);
                    if (createOption == null) return event.reply(InteractionApplicationCommandCallbackSpec
                            .builder()
                            .ephemeral(true)
                            .content("Invalid command")
                            .build());
                    var channelOption = createOption.getOption("channel").orElse(null);
                    if (channelOption == null) return event.reply(InteractionApplicationCommandCallbackSpec
                            .builder()
                            .ephemeral(true)
                            .content("No channel")
                            .build());
                    var nameOption = createOption.getOption("name").orElse(null);
                    if (nameOption == null) return event.reply(InteractionApplicationCommandCallbackSpec
                            .builder()
                            .ephemeral(true)
                            .content("No name")
                            .build());
                    var intervalOption = createOption.getOption("interval").orElse(null);
                    if (intervalOption == null) return event.reply(InteractionApplicationCommandCallbackSpec
                            .builder()
                            .ephemeral(true)
                            .content("No interval")
                            .build());
                    var startTimeOption = createOption.getOption("starttime").orElse(null);
                    if (startTimeOption == null) return event.reply(InteractionApplicationCommandCallbackSpec
                            .builder()
                            .ephemeral(true)
                            .content("No starttime")
                            .build());
                    var targetRoleOption = createOption.getOption("targetrole").orElse(null);
                    if (targetRoleOption == null) return event.reply(InteractionApplicationCommandCallbackSpec
                            .builder()
                            .ephemeral(true)
                            .content("No targetrole")
                            .build());
                    return channelOption.getValue().orElseThrow().asChannel().flatMap(c -> {
                        if (c instanceof MessageChannel) {
                            return targetRoleOption.getValue().orElseThrow().asRole().flatMap(role -> {
                                var name = nameOption.getValue().orElseThrow().asString();
                                var interval = intervalOption.getValue().orElseThrow().asLong();
                                var startTime = startTimeOption.getValue().orElseThrow().asLong();
                                var roleId = role.getId().asLong();
                                var channelId = c.getId().asLong();
                                var reminder = new Reminder(name, startTime, interval, channelId, roleId);
                                var suc = ReminderBot.addReminder(reminder);
                                if (!suc) {
                                    return event.reply(InteractionApplicationCommandCallbackSpec
                                            .builder()
                                            .content("A reminder with that name already exists")
                                            .build());
                                } else {
                                    return event.reply(InteractionApplicationCommandCallbackSpec
                                            .builder()
                                            .content("Reminder created!")
                                            .build());
                                }
                            });
                        } else {
                            return event.reply(InteractionApplicationCommandCallbackSpec
                                    .builder()
                                    .ephemeral(true)
                                    .content("Not a Message channel")
                                    .build());
                        }
                    });
                }
                case "list" -> {
                    var reminders = ReminderBot.getReminders();
                    var text = "Reminders (" + reminders.size() + "):" + reminders
                            .stream()
                            .map(Reminder::name)
                            .map(s -> " - " + s)
                            .collect(Collectors.joining("\n"));
                    return event.reply(InteractionApplicationCommandCallbackSpec
                            .builder()
                            .content(text)
                            .ephemeral(true)
                            .build());
                }
            }
            return event.reply("Done");
        }).subscribe();
    }
}
