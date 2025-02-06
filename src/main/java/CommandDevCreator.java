import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CommandDevCreator {
    private static GatewayDiscordClient gateway;
    private static long appId;

    public static void main(String[] args) {
        gateway = ReminderBot.login(args[0]);
        appId = Objects.requireNonNull(gateway.getRestClient().getApplicationId().block());

        createCommands();
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
