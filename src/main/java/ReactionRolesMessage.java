import java.util.HashMap;
import java.util.Map;

public class ReactionRolesMessage {
    private final String msgId;
    private final long channelId;
    private final long messageId;
    private final Map<Long, RoleEntry> roles = new HashMap<>();
    private String content;

    public ReactionRolesMessage(String msgId, long channelId, long messageId, String content) {
        this.msgId = msgId;
        this.channelId = channelId;
        this.messageId = messageId;
        this.content = content;
    }

    public long getChannelId() {
        return channelId;
    }

    public long getMessageId() {
        return messageId;
    }

    public void content(String content) {
        this.content = content;
    }

    public String getMsgId() {
        return msgId;
    }

    public String getContent() {
        return content;
    }

    public Map<Long, RoleEntry> getRoles() {
        return roles;
    }

    public record RoleEntry(long roleId, String display) {
    }
}
