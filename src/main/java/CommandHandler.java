import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;

public class CommandHandler {
    private static final Logger LOGGER = Loggers.getLogger(CommandHandler.class);

    public static void register(GatewayDiscordClient gateway) {
        gateway.on(ChatInputInteractionEvent.class, event -> {
            var guildIdOptional = event.getInteraction().getGuildId();
            if (guildIdOptional.isEmpty()) {
                return event.reply("Bad input");
            }
            var instance = ServerManager.INSTANCE.get(guildIdOptional.get().asLong());
            try {
                var command = event.getCommandName();
                switch (command) {
                    case "event" -> {
                        var subName = event.getInteraction().getData().data().get().options().get().getFirst().name();
                        switch (subName) {
                            case "create" -> {
                                var createOption = event.getOption("create").orElseThrow();
                                var name = createOption
                                        .getOption("name")
                                        .orElseThrow()
                                        .getValue()
                                        .orElseThrow()
                                        .asString();
                                var intervalStr = createOption
                                        .getOption("interval")
                                        .orElseThrow()
                                        .getValue()
                                        .orElseThrow()
                                        .asString();

                                Duration interval;
                                try {
                                    interval = DurationParser.parse(intervalStr);
                                } catch (IllegalArgumentException e) {
                                    return event.reply(InteractionApplicationCommandCallbackSpec
                                            .builder()
                                            .content("Failed to parse interval: " + e.getMessage() + "\nValid examples:\n - 4w+1d-5m+1s\n - 2d\n - 4w")
                                            .ephemeral(true)
                                            .build());
                                }

                                var firstStartStr = createOption
                                        .getOption("firststart")
                                        .orElseThrow()
                                        .getValue()
                                        .orElseThrow()
                                        .asString();
                                Instant firstStart;
                                try {
                                    firstStart = InstantParser.parse(firstStartStr);
                                } catch (DateTimeParseException e) {
                                    return event.reply(InteractionApplicationCommandCallbackSpec
                                            .builder()
                                            .content("Failed to parse firstStart: " + e.getMessage() + "\nValid examples:\n - 16/09/2025 19:00:05\n - 17/09 19:00\n - 18/09 19:00:05")
                                            .ephemeral(true)
                                            .build());
                                }
                                try {
                                    return instance.useFun(i -> {
                                        if (!i.addEvent(name, firstStart, interval)) {
                                            return event.reply(InteractionApplicationCommandCallbackSpec
                                                    .builder()
                                                    .content("An event with that name already exists")
                                                    .ephemeral(true)
                                                    .build());
                                        }
                                        return event.reply(InteractionApplicationCommandCallbackSpec
                                                .builder()
                                                .content("Event created")
                                                .ephemeral(true)
                                                .build());
                                    });
                                } catch (IOException e) {
                                    LOGGER.error("Failed to save config", e);
                                    return event.reply(InteractionApplicationCommandCallbackSpec
                                            .builder()
                                            .content("Internal error: Failed to save config. Contact DasBabyPixel")
                                            .ephemeral(true)
                                            .build());
                                }
                            }
                            case "manage" -> {
                                var subOption = event.getOption("manage").orElseThrow().getOptions().getFirst();
                                switch (subOption.getName()) {
                                    case "move" -> {
                                        return event.reply(InteractionApplicationCommandCallbackSpec
                                                .builder()
                                                .content("Move")
                                                .ephemeral(true)
                                                .build());
                                    }
                                    case "move_specific" -> {
                                        return event.reply(InteractionApplicationCommandCallbackSpec
                                                .builder()
                                                .content("Move specific")
                                                .ephemeral(true)
                                                .build());
                                    }
                                }
                            }
                            case "info" -> {
                                var eventName = event
                                        .getOption("info")
                                        .orElseThrow()
                                        .getOption("name")
                                        .orElseThrow()
                                        .getValue()
                                        .orElseThrow()
                                        .asString();
                                return instance.useFun(i -> {
                                    if (!i.events().containsKey(eventName)) {
                                        return event.reply(InteractionApplicationCommandCallbackSpec
                                                .builder()
                                                .content("Event named " + eventName + " not found")
                                                .ephemeral(true)
                                                .build());
                                    }
                                    var e = i.events().get(eventName);
                                    return event.reply(InteractionApplicationCommandCallbackSpec
                                            .builder()
                                            .content("""
                                                    %1$s:
                                                     - Interval: %2$s
                                                     - Reminders: %3$d
                                                     - Next Execution: %4$s
                                                    """.formatted(e.name(), DurationParser.toDisplayString(e.interval()), e
                                                    .reminders()
                                                    .size(), DateTimeFormatter
                                                    .ofPattern("dd/MM/yy HH:mm:ss")
                                                    .withZone(ZoneOffset.UTC)
                                                    .format(e.getNextEventTime(Instant.now())) + " UTC"))
                                            .ephemeral(true)
                                            .build());
                                });
                            }
                            case "list" -> {
                                return instance.useFun(i -> {
                                    var events = i.events();
                                    var text = "Events (" + events.size() + "):" + events
                                            .values()
                                            .stream()
                                            .map(e -> " - " + e.name() + " (" + e.reminders().size() + " reminders)")
                                            .collect(Collectors.joining("\n", "\n", ""));
                                    return event.reply(InteractionApplicationCommandCallbackSpec
                                            .builder()
                                            .content(text)
                                            .ephemeral(true)
                                            .build());
                                });
                            }
                            case "delete" -> {
                                var deleteOption = event.getOption("delete").orElseThrow();
                                var name = deleteOption
                                        .getOption("name")
                                        .orElseThrow()
                                        .getValue()
                                        .orElseThrow()
                                        .asString();
                                try {
                                    return instance.useFun(i -> {
                                        if (!i.removeEvent(name)) {
                                            return event.reply(InteractionApplicationCommandCallbackSpec
                                                    .builder()
                                                    .content("No event with that name could be found")
                                                    .ephemeral(true)
                                                    .build());
                                        }
                                        return event.reply(InteractionApplicationCommandCallbackSpec
                                                .builder()
                                                .content("Event deleted")
                                                .ephemeral(true)
                                                .build());
                                    });
                                } catch (IOException e) {
                                    LOGGER.error("Failed to save config", e);
                                    return event.reply(InteractionApplicationCommandCallbackSpec
                                            .builder()
                                            .content("Internal error: Failed to save config. Contact DasBabyPixel")
                                            .ephemeral(true)
                                            .build());
                                }
                            }
                        }
                        return event.reply(InteractionApplicationCommandCallbackSpec
                                .builder()
                                .content("Unsupported command")
                                .ephemeral(true)
                                .build());
                    }
                    case "reminder" -> {
                        var subName = event.getInteraction().getData().data().get().options().get().getFirst().name();
                        var op = event.getOption(subName).orElseThrow();
                        switch (subName) {
                            case "create" -> {
                                var eventName = op.getOption("event").orElseThrow().getValue().orElseThrow().asString();
                                var name = op.getOption("name").orElseThrow().getValue().orElseThrow().asString();
                                var offsetStr = op
                                        .getOption("offset")
                                        .orElseThrow()
                                        .getValue()
                                        .orElseThrow()
                                        .asString();
                                Duration offset;
                                try {
                                    offset = DurationParser.parse(offsetStr);
                                } catch (IllegalArgumentException e) {
                                    return event.reply(InteractionApplicationCommandCallbackSpec
                                            .builder()
                                            .content("Failed to parse offset: " + e.getMessage() + "\nValid examples:\n - 5m\n - 10m\n - 1h")
                                            .ephemeral(true)
                                            .build());
                                }
                                var message = op.getOption("message").orElseThrow().getValue().orElseThrow().asString();
                                return op
                                        .getOption("channel")
                                        .orElseThrow()
                                        .getValue()
                                        .orElseThrow()
                                        .asChannel()
                                        .flatMap(channel -> instance.useFun(i -> {
                                            try {
                                                i.addReminder(eventName, name, offset, message, channel
                                                        .getId()
                                                        .asLong());
                                                return event.reply(InteractionApplicationCommandCallbackSpec
                                                        .builder()
                                                        .content("Reminder created")
                                                        .ephemeral(true)
                                                        .build());
                                            } catch (IOException e) {
                                                LOGGER.error("Failed to save config", e);
                                                return event.reply(InteractionApplicationCommandCallbackSpec
                                                        .builder()
                                                        .content("Internal error: Failed to save config. Contact DasBabyPixel")
                                                        .ephemeral(true)
                                                        .build());
                                            } catch (IllegalArgumentException e) {
                                                return event.reply(InteractionApplicationCommandCallbackSpec
                                                        .builder()
                                                        .content(e.getMessage())
                                                        .ephemeral(true)
                                                        .build());
                                            }
                                        }));
                            }
                            case "list" -> {
                                var eventName = op.getOption("event").orElseThrow().getValue().orElseThrow().asString();
                                return instance.useFun(i -> {
                                    var e = i.events().get(eventName);
                                    if (e == null) return event.reply(InteractionApplicationCommandCallbackSpec
                                            .builder()
                                            .content("No event named " + eventName + " exists")
                                            .ephemeral(true)
                                            .build());
                                    var rems = e.reminders().values();
                                    var text = "Reminders for " + eventName + " (" + rems.size() + "):" + rems
                                            .stream()
                                            .map(r -> " - " + r.name() + " (" + DurationParser.toDisplayString(r.offsetBeforeEvent()) + ")")
                                            .collect(Collectors.joining("\n", "\n", ""));
                                    return event.reply(InteractionApplicationCommandCallbackSpec
                                            .builder()
                                            .content(text)
                                            .ephemeral(true)
                                            .build());
                                });
                            }
                            case "delete" -> {
                                var eventName = op.getOption("event").orElseThrow().getValue().orElseThrow().asString();
                                var name = op.getOption("name").orElseThrow().getValue().orElseThrow().asString();
                                return instance.useFun(i -> {
                                    try {
                                        if (i.removeReminder(eventName, name)) {
                                            return event.reply(InteractionApplicationCommandCallbackSpec
                                                    .builder()
                                                    .content("Reminder deleted")
                                                    .ephemeral(true)
                                                    .build());
                                        } else {
                                            return event.reply(InteractionApplicationCommandCallbackSpec
                                                    .builder()
                                                    .content("No reminder named " + name + " was found for event " + eventName)
                                                    .ephemeral(true)
                                                    .build());
                                        }
                                    } catch (IOException e) {
                                        LOGGER.error("Failed to save config", e);
                                        return event.reply(InteractionApplicationCommandCallbackSpec
                                                .builder()
                                                .content("Internal error: Failed to save config. Contact DasBabyPixel")
                                                .ephemeral(true)
                                                .build());
                                    } catch (IllegalArgumentException e) {
                                        return event.reply(InteractionApplicationCommandCallbackSpec
                                                .builder()
                                                .content(e.getMessage())
                                                .ephemeral(true)
                                                .build());
                                    }
                                });
                            }
                        }
                    }
                }
                return event.reply(InteractionApplicationCommandCallbackSpec
                        .builder()
                        .content("Unknown command: " + command)
                        .ephemeral(true)
                        .build());
            } catch (Error t) {
                t.printStackTrace();
                throw t;
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw e;
            }
        }).subscribe();
    }
}
