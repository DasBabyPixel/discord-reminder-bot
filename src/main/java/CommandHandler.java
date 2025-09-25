import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.core.spec.InteractionReplyEditSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class CommandHandler {
    private static final Logger LOGGER = Loggers.getLogger(CommandHandler.class);

    public static void register(GatewayDiscordClient gateway) {
        gateway.on(ButtonInteractionEvent.class, event -> {
            var customIdString = event.getCustomId();
            System.out.println("Button press: " + customIdString);
            if (customIdString.startsWith("reactionroles/")) {
                var sub = customIdString.substring("reactionroles/".length()); // without prefix
                var idx = sub.indexOf('/');
                var roleId = 0L;
                var display = "";
                if (idx != -1) {
                    var displayLen = Integer.parseInt(sub.substring(0, idx));
                    sub = sub.substring(idx + 1); // without "<length>/"
                    display = sub.substring(0, displayLen);
                    sub = sub.substring(displayLen + 1); // without "<display>/"
                    roleId = Long.parseLong(sub);
                }
                var guildIdOptional = event.getInteraction().getGuildId();
                System.out.println("Conditions: " + guildIdOptional.isPresent() + " " + idx);
                if (guildIdOptional.isPresent() && idx != -1) {
                    var froleId = roleId;
                    var fdispaly = display;
                    var member = event.getInteraction().getMember().orElse(null);
                    System.out.println("Member: " + (member == null ? null : member.getDisplayName()));
                    if (member != null) {
                        return member
                                .getRoles()
                                .any(r -> r.getId().asLong() == froleId)
                                .flatMap(has -> {
                                    System.out.println("Member has role: " + has);
                                    if (has) {
                                        return member
                                                .removeRole(Snowflake.of(froleId))
                                                .thenReturn("Your role for " + fdispaly + " was removed");
                                    } else {
                                        return member
                                                .addRole(Snowflake.of(froleId))
                                                .thenReturn("You were given the role for " + fdispaly);
                                    }
                                })
                                .flatMap(s -> event.reply(InteractionApplicationCommandCallbackSpec
                                        .builder()
                                        .ephemeral(true)
                                        .content(s)
                                        .build()));
                    }
                }
            }
            return event.reply(InteractionApplicationCommandCallbackSpec
                    .builder()
                    .ephemeral(true)
                    .content("Unknown button. Contact DasBabyPixel or server admins")
                    .build());
        }).subscribe();
        gateway.on(ChatInputAutoCompleteEvent.class, event -> {
            try {
                var focusedOption = event.getFocusedOption();
                var guildIdOptional = event.getInteraction().getGuildId();
                if (guildIdOptional.isEmpty()) {
                    return event.respondWithSuggestions(Set.of());
                }
                var instance = ServerManager.INSTANCE.get(guildIdOptional.get().asLong());
                if (focusedOption.getName().equals("event")) {
                    var input = focusedOption
                            .getValue()
                            .map(ApplicationCommandInteractionOptionValue::asString)
                            .orElse("");
                    return event.respondWithSuggestions(instance.useFun(i -> i
                            .events()
                            .keySet()
                            .stream()
                            .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(input.toLowerCase(Locale.ROOT)))
                            .map(n -> (ApplicationCommandOptionChoiceData) ApplicationCommandOptionChoiceData
                                    .builder()
                                    .name(n)
                                    .value(n)
                                    .build())
                            .toList()));
                } else if (focusedOption.getName().equals("reminder")) {
                    var firstLevel = event.getOptions().stream().findFirst().orElse(null);
                    if (firstLevel == null) return event.respondWithSuggestions(Set.of());

                    var eventName = firstLevel
                            .getOption("event")
                            .flatMap(ApplicationCommandInteractionOption::getValue)
                            .map(ApplicationCommandInteractionOptionValue::asString)
                            .orElse(null);
                    if (eventName == null) {
                        return event.respondWithSuggestions(Set.of());
                    }
                    var input = focusedOption
                            .getValue()
                            .map(ApplicationCommandInteractionOptionValue::asString)
                            .orElse("");
                    return event.respondWithSuggestions(instance.useFun(i -> {
                        var eventData = i.events().get(eventName);
                        if (eventData == null) return Set.of();

                        return eventData
                                .reminders()
                                .keySet()
                                .stream()
                                .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(input.toLowerCase(Locale.ROOT)))
                                .map(n -> (ApplicationCommandOptionChoiceData) ApplicationCommandOptionChoiceData
                                        .builder()
                                        .name(n)
                                        .value(n)
                                        .build())
                                .toList();
                    }));
                } else if (focusedOption.getName().equals("message-id")) {
                    var input = focusedOption
                            .getValue()
                            .map(ApplicationCommandInteractionOptionValue::asString)
                            .orElse("");

                    return event.respondWithSuggestions(instance.useFun(i -> i
                            .reactionRoles()
                            .keySet()
                            .stream()
                            .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(input.toLowerCase(Locale.ROOT)))
                            .map(n -> (ApplicationCommandOptionChoiceData) ApplicationCommandOptionChoiceData
                                    .builder()
                                    .name(n)
                                    .value(n)
                                    .build())
                            .toList()));
                }
                return event.respondWithSuggestions(Set.of());
            } catch (Throwable t) {
                LOGGER.error("Failed autocomplete", t);
                return event.respondWithSuggestions(Set.of());
            }
        }).subscribe();
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
                                    case "move-to" -> {
                                        var eventName = subOption
                                                .getOption("event")
                                                .orElseThrow()
                                                .getValue()
                                                .orElseThrow()
                                                .asString();
                                        var firstStartStr = subOption
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

                                        var msg = instance.useFun(i -> {
                                            try {
                                                if (!i.moveEventTo(eventName, firstStart)) {
                                                    return "There is no event " + eventName;
                                                }
                                            } catch (IOException e) {
                                                LOGGER.error("Failed to save config", e);
                                                return "Internal error: Failed to save config. Contact DasBabyPixel";
                                            }
                                            return "The event was moved. Use /event info for more information";
                                        });
                                        return event.reply(InteractionApplicationCommandCallbackSpec
                                                .builder().content(msg)
                                                .ephemeral(true)
                                                .build());
                                    }
//                                    case "move_specific" -> {
//                                        return event.reply(InteractionApplicationCommandCallbackSpec
//                                                .builder()
//                                                .content("Move specific")
//                                                .ephemeral(true)
//                                                .build());
//                                    }
                                    case "move-by-once" -> {
                                        var eventName = subOption
                                                .getOption("event")
                                                .orElseThrow()
                                                .getValue()
                                                .orElseThrow()
                                                .asString();

                                        var offsetStr = subOption
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
                                                    .content("Failed to parse offset: " + e.getMessage() + "\nValid examples:\n - 4w+1d-5m+1s\n - 2d\n - 4w")
                                                    .ephemeral(true)
                                                    .build());
                                        }

                                        var msg = instance.useFun(i -> {
                                            try {
                                                if (!i.moveEventByOnce(eventName, offset)) {
                                                    return "There is no event " + eventName;
                                                }
                                            } catch (IOException e) {
                                                LOGGER.error("Failed to save config", e);
                                                return "Internal error: Failed to save config. Contact DasBabyPixel";
                                            }
                                            return "The event was moved. Use /event info for more information";
                                        });
                                        return event.reply(InteractionApplicationCommandCallbackSpec
                                                .builder()
                                                .content(msg)
                                                .ephemeral(true)
                                                .build());
                                    }
                                }
                            }
                            case "info" -> {
                                var eventName = event.getOption("info").orElseThrow().getOption("event")
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
                                    var now = Instant.now();
                                    var next = e.getNextEventTime(now);
                                    var delay = now.until(next);
                                    return event.reply(InteractionApplicationCommandCallbackSpec
                                            .builder()
                                            .content("""
                                                    %1$s:
                                                     - Interval: %2$s
                                                     - Reminders: %3$d
                                                     - Next Execution: %4$s (in %5$s)
                                                    """.formatted(e.name(), DurationParser.toDisplayString(e.interval()), e
                                                    .reminders()
                                                    .size(), DateTimeFormatter
                                                    .ofPattern("dd/MM/yy HH:mm:ss")
                                                    .withZone(ZoneOffset.UTC)
                                                    .format(next) + " UTC", DurationParser.toDisplayString(delay)))
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
                                var name = deleteOption.getOption("event")
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
                                var reminder = op
                                        .getOption("reminder")
                                        .orElseThrow()
                                        .getValue()
                                        .orElseThrow()
                                        .asString();
                                return instance.useFun(i -> {
                                    try {
                                        if (i.removeReminder(eventName, reminder)) {
                                            return event.reply(InteractionApplicationCommandCallbackSpec
                                                    .builder()
                                                    .content("Reminder deleted")
                                                    .ephemeral(true)
                                                    .build());
                                        } else {
                                            return event.reply(InteractionApplicationCommandCallbackSpec
                                                    .builder()
                                                    .content("No reminder named " + reminder + " was found for event " + eventName)
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
                    case "reactionroles" -> {
                        var subNameRR = event.getInteraction().getData().data().get().options().get().getFirst().name();
                        var opRR = event.getOption(subNameRR).orElseThrow();
                        switch (subNameRR) {
                            case "message" -> {
                                var opM = opRR.getOptions().getFirst();
                                var subNameM = opM.getName();
                                switch (subNameM) {
                                    case "create" -> {
                                        var msgId = opM
                                                .getOption("message-id")
                                                .orElseThrow()
                                                .getValue()
                                                .orElseThrow()
                                                .asString();
                                        var channelFlake = opM
                                                .getOption("channel")
                                                .orElseThrow()
                                                .getValue()
                                                .orElseThrow()
                                                .asSnowflake();
                                        var channelId = channelFlake.asLong();
                                        var content = opM
                                                .getOption("content")
                                                .orElseThrow()
                                                .getValue()
                                                .orElseThrow()
                                                .asString();
                                        return instance.useFun(i -> {
                                                    if (i.reactionRoles().containsKey(msgId)) {
                                                        // update
                                                        try {
                                                            var messageId = i.updateReactionRolesMessage(msgId, channelId, content);
                                                            var spec = buildRoleEditSpec(i.reactionRoles().get(msgId));
                                                            return gateway
                                                                    .getChannelById(channelFlake)
                                                                    .map(c -> (MessageChannel) c)
                                                                    .flatMap(c -> c.getMessageById(Snowflake.of(messageId)))
                                                                    .flatMap(m -> m.edit(spec))
                                                                    .thenReturn("Updated message");
                                                        } catch (IOException e) {
                                                            LOGGER.error("Failed to save config", e);
                                                            return Mono.just("Internal error: Failed to save config. Contact DasBabyPixel");
                                                        }
                                                    }
                                                    return gateway
                                                            .getChannelById(channelFlake)
                                                            .map(c -> (MessageChannel) c)
                                                            .flatMap(c -> c.createMessage(buildRoleCreateSpec(new ReactionRolesMessage(msgId, channelId, 0L, content))))
                                                            .map(m -> {
                                                                try {
                                                                    instance.useCal(c -> c.addReactionRolesMessage(msgId, channelId, m
                                                                            .getId()
                                                                            .asLong(), content));
                                                                    return "Created reaction roles message";
                                                                } catch (IOException e) {
                                                                    LOGGER.error("Failed to save config", e);
                                                                    return "Internal error: Failed to save config. Contact DasBabyPixel";
                                                                }
                                                            });
                                                })
                                                .flatMap(s -> event.reply(InteractionApplicationCommandCallbackSpec
                                                        .builder()
                                                        .content(s)
                                                        .ephemeral(true)
                                                        .build()));
                                    }
                                    case "delete" -> {
                                        var msgId = opM
                                                .getOption("message-id")
                                                .orElseThrow()
                                                .getValue()
                                                .orElseThrow()
                                                .asString();
                                        return instance
                                                .useFun(i -> {
                                                    try {
                                                        var rr = i.reactionRoles().get(msgId);
                                                        if (!i.removeReactionRolesMessage(msgId)) {
                                                            return Mono.just("There is no message with id `" + msgId + "`");
                                                        }
                                                        Objects.requireNonNull(rr);
                                                        return gateway
                                                                .getChannelById(Snowflake.of(rr.getChannelId()))
                                                                .map(c -> (MessageChannel) c)
                                                                .flatMap(channel -> channel.getMessageById(Snowflake.of(rr.getMessageId())))
                                                                .flatMap(Message::delete)
                                                                .then(Mono.just("Deleted message with id " + msgId));
                                                    } catch (IOException e) {
                                                        LOGGER.error("Failed to save config", e);
                                                        return Mono.just("Internal error: Failed to save config. Contact DasBabyPixel");
                                                    }
                                                })
                                                .flatMap(content -> event.reply(InteractionApplicationCommandCallbackSpec
                                                        .builder()
                                                        .content(content)
                                                        .ephemeral(true)
                                                        .build()));
                                    }
                                    case "list" -> {
                                        return instance.useFun(i -> {
                                            var messages = i.reactionRoles();
                                            var text = "Reaction Messages (" + messages.size() + "):" + messages
                                                    .values()
                                                    .stream()
                                                    .map(e -> " - " + e.getMsgId())
                                                    .collect(Collectors.joining("\n", "\n", ""));
                                            return event.reply(InteractionApplicationCommandCallbackSpec
                                                    .builder()
                                                    .content(text)
                                                    .ephemeral(true)
                                                    .build());
                                        });
                                    }
                                }
                            }
                            case "roles" -> {
                                var opR = opRR.getOptions().getFirst();
                                var subNameR = opR.getName();
                                switch (subNameR) {
                                    case "add" -> {
                                        var msgId = opR
                                                .getOption("message-id")
                                                .orElseThrow()
                                                .getValue()
                                                .orElseThrow()
                                                .asString();
                                        var roleFlake = opR
                                                .getOption("role")
                                                .orElseThrow()
                                                .getValue()
                                                .orElseThrow()
                                                .asSnowflake();
                                        var display = opR
                                                .getOption("display")
                                                .orElseThrow()
                                                .getValue()
                                                .orElseThrow()
                                                .asString();
                                        return instance
                                                .useFun(i -> {
                                                    try {
                                                        if (!i.reactionRoles().containsKey(msgId)) {
                                                            return Mono.just("Unknown message id: " + msgId);
                                                        }
                                                        var rr = Objects.requireNonNull(i.reactionRoles().get(msgId));
                                                        if (rr.getRoles().containsKey(roleFlake.asLong())) {
                                                            i.updateRoleDisplay(msgId, roleFlake.asLong(), display);
                                                            return gateway
                                                                    .getChannelById(Snowflake.of(rr.getChannelId()))
                                                                    .map(c -> (MessageChannel) c)
                                                                    .flatMap(c -> c.getMessageById(Snowflake.of(rr.getMessageId())))
                                                                    .flatMap(m -> m.edit(buildRoleEditSpec(rr)))
                                                                    .then(Mono.just("Updated role display for message " + msgId));
                                                        } else {
                                                            i.addReactionRolesRole(msgId, roleFlake.asLong(), display);
                                                            return gateway
                                                                    .getChannelById(Snowflake.of(rr.getChannelId()))
                                                                    .map(c -> (MessageChannel) c)
                                                                    .flatMap(c -> c.getMessageById(Snowflake.of(rr.getMessageId())))
                                                                    .flatMap(m -> m.edit(buildRoleEditSpec(rr)))
                                                                    .then(Mono.just("Added role to message " + msgId));
                                                        }
                                                    } catch (IOException e) {
                                                        LOGGER.error("Failed to save config", e);
                                                        return Mono.just("Internal error: Failed to save config. Contact DasBabyPixel");
                                                    }
                                                })
                                                .flatMap(s -> event.reply(InteractionApplicationCommandCallbackSpec
                                                        .builder()
                                                        .content(s)
                                                        .ephemeral(true)
                                                        .build()));
                                    }
                                    case "remove" -> {
                                        var msgId = opR
                                                .getOption("message-id")
                                                .orElseThrow()
                                                .getValue()
                                                .orElseThrow()
                                                .asString();
                                        var roleFlake = opR
                                                .getOption("role")
                                                .orElseThrow()
                                                .getValue()
                                                .orElseThrow()
                                                .asSnowflake();

                                        return instance
                                                .useFun(i -> {
                                                    try {
                                                        if (!i.reactionRoles().containsKey(msgId)) {
                                                            return Mono.just("Unknown message id: " + msgId);
                                                        }
                                                        var rr = Objects.requireNonNull(i.reactionRoles().get(msgId));
                                                        if (!rr.getRoles().containsKey(roleFlake.asLong())) {
                                                            return Mono.just("The message " + msgId + " does not have that role");
                                                        }
                                                        i.removeReactionRolesRole(msgId, roleFlake.asLong());
                                                        return gateway
                                                                .getChannelById(Snowflake.of(rr.getChannelId()))
                                                                .map(c -> (MessageChannel) c)
                                                                .flatMap(c -> c.getMessageById(Snowflake.of(rr.getMessageId())))
                                                                .flatMap(m -> m.edit(buildRoleEditSpec(rr)))
                                                                .then(Mono.just("Removed role from message " + msgId));
                                                    } catch (IOException e) {
                                                        LOGGER.error("Failed to save config", e);
                                                        return Mono.just("Internal error: Failed to save config. Contact DasBabyPixel");
                                                    }
                                                })
                                                .flatMap(s -> event.reply(InteractionApplicationCommandCallbackSpec
                                                        .builder()
                                                        .content(s)
                                                        .ephemeral(true)
                                                        .build()));
                                    }
                                }
                            }
                        }
                    }
                }
                return event.reply(InteractionApplicationCommandCallbackSpec
                        .builder()
                        .content("Unknown command: " + command)
                        .ephemeral(true)
                        .build());
            } catch (Throwable t) {
                LOGGER.error("Error during command", t);
                throw t;
            }
        }).subscribe();
    }

    private static List<LayoutComponent> buildRoleSpecComponents(ReactionRolesMessage msg) {
        var components = new ArrayList<LayoutComponent>();
        var rowContent = new ArrayList<Button>(4);
        var roles = msg.getRoles().values().stream() // TODO better sorting through customizable weight
                .sorted(Comparator.comparing(ReactionRolesMessage.RoleEntry::display)).toList();
        for (var role : roles) {
            var customId = new StringBuilder();
            customId.append("reactionroles/");
            var display = role.display();
            customId.append(display.length());
            customId.append('/');
            customId.append(display);
            customId.append('/');
            customId.append(role.roleId());

            var button = Button.primary(customId.toString(), role.display());
            rowContent.add(button);
            if (rowContent.size() == 4) {
                components.add(ActionRow.of(List.copyOf(rowContent)));
                rowContent.clear();
            }
        }
        if (!rowContent.isEmpty()) components.add(ActionRow.of(List.copyOf(rowContent)));
        return List.copyOf(components);
    }

    private static MessageEditSpec buildRoleEditSpec(ReactionRolesMessage msg) {
        var components = buildRoleSpecComponents(msg);
        return MessageEditSpec.builder().contentOrNull(msg.getContent()).components(components).build();
    }

    private static MessageCreateSpec buildRoleCreateSpec(ReactionRolesMessage msg) {
        var components = buildRoleSpecComponents(msg);
        return MessageCreateSpec.builder().content(msg.getContent()).components(components).build();
    }
}
