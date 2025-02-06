import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.MessageCreateSpec;

public class CreateMessage {
    public static void main(String[] args) {
        var gateway = ReminderBot.login(args[0]);

//        var cid = 849313475518922762L; // dev
        var cid = 1337148665675907104L;
        var c = (MessageChannel) gateway.getChannelById(Snowflake.of(cid)).block();
        var bbear1 = Button.primary("bear1", "Bear 1");
        var bbear2 = Button.primary("bear2", "Bear 2");
        var reset = Button.primary("reset", "Reset");
        c
                .createMessage(MessageCreateSpec
                        .builder()
                        .content("Select your reminders")
                        .addComponent(ActionRow.of(bbear1, bbear2, reset))
                        .build())
                .subscribe();
    }
}
