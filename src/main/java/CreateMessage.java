import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;

import java.util.List;

public class CreateMessage {
    public static void main(String[] args) {
        var gateway = ReminderBot.login(args[0]);

//        var cid = 849313475518922762L; // dev
        var cid = 1337148665675907104L;
        var c = (MessageChannel) gateway.getChannelById(Snowflake.of(cid)).block();
        var bbear1 = Button.primary("bear1", "Bear 1");
        var bbear2 = Button.primary("bear2", "Bear 2");
        var reset3 = Button.primary("reset-3", "Reset 3 Min");
        var reset5 = Button.primary("reset-5", "Reset 5 Min");
        var reset10 = Button.primary("reset-10", "Reset 10 Min");
        var reset20 = Button.primary("reset-20", "Reset 20 Min");
        var spec = MessageEditSpec
                .builder()
                .content("Select your reminders")
                .components(List.of(ActionRow.of(bbear1, bbear2), ActionRow.of(reset3, reset5, reset10, reset20)))
                .build();
        c.getMessageById(Snowflake.of(1337154531896721541L)).block().edit(spec).block();
//        c.createMessage(spec).block();
    }
}
