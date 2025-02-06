import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConsoleReader {
    private static final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    public static void start() {
        Thread.ofPlatform().start(() -> {
            while (true) {
                try {
                    var line = reader.readLine();
                    if (line == null) continue;
                    if (line.equals("exit")) break;
                    System.out.println("Read: " + line);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
