import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Locale;

public class InstantParser {
    private static final ZoneId UTC = ZoneOffset.UTC;

    public static Instant parse(String input) throws DateTimeParseException {
        var formatter = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.DAY_OF_MONTH, 2)
                .appendLiteral('/')
                .appendValue(ChronoField.MONTH_OF_YEAR, 2)
                .parseDefaulting(ChronoField.YEAR, LocalDateTime.now(UTC).getYear())
                .optionalStart()
                .appendLiteral('/')
                .appendValue(ChronoField.YEAR, 2)
                .optionalEnd()
                .appendLiteral(' ')
                .appendValue(ChronoField.HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                .optionalStart()
                .appendLiteral(':')
                .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                .optionalEnd()
                .toFormatter(Locale.ROOT);
        return LocalDateTime.parse(input, formatter).atZone(UTC).toInstant();
    }
}
