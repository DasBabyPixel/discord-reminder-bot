import discord4j.core.DiscordClient;

import java.util.Objects;

public class ReminderBot {
    public static void main(String[] args) {
        var token = args[0];
        var client = DiscordClient.create(token);
        var gateway = Objects.requireNonNull(client.login().block());
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> gateway.logout().block()));
    }
}
