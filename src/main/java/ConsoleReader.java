import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.format.DateTimeParseException;

public class ConsoleReader {
    private static final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    public static void start() {
        Thread.ofPlatform().start(() -> {
            while (true) {
                try {
                    var line = reader.readLine();
                    if (line == null) continue;
                    if (line.equals("exit")) break;
                    if (line.startsWith("dur: ")) {
                        var durStr = line.substring("dur: ".length());
                        try {
                            var dur = DurationParser.parse(durStr);
                            System.out.println("Duration: " + dur);
                            System.out.println("DurationStr: " + DurationParser.toDisplayString(dur));
                        } catch (IllegalArgumentException e) {
                            System.out.println(e.getMessage());
                        }
                    } else if (line.startsWith("time: ")) {
                        var str = line.substring("time: ".length());
                        try {
                            var i = InstantParser.parse(str);
                            System.out.println(i);
                        } catch (DateTimeParseException e) {
                            System.out.println(e.getMessage());
                        }
                    } else {
                        System.out.println("Read: " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
