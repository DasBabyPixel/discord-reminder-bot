import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ServerManager {
    public static final ServerManager INSTANCE = new ServerManager();

    private final Gson gson = new Gson();
    private final Map<Long, ServerInstance> servers = new HashMap<>();
    private final Path serversPath;

    private ServerManager() {
        serversPath = Path.of("servers");
    }

    public ServerInstance get(long id) {
        return servers.computeIfAbsent(id, ServerInstance::new);
    }

    public void save(ServerInstance instance) throws IOException {
        var path = serversPath.resolve(Long.toString(instance.id()));
        Files.createDirectories(serversPath);
        try (var writer = Files.newBufferedWriter(path)) {
            var json = new JsonArray();
            for (var reminder : instance.reminders().values()) {
                var rj = new JsonObject();

            }
            gson.toJson(json, writer);
        }
    }
}
