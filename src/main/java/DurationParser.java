import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class DurationParser {
    public static String toDisplayString(Duration duration) {
        var d = duration;
        var weeks = d.dividedBy(ChronoUnit.WEEKS.getDuration());
        d = d.minusDays(weeks * 7);
        var days = d.dividedBy(ChronoUnit.DAYS.getDuration());
        d = d.minusDays(days);
        var hours = d.dividedBy(ChronoUnit.HOURS.getDuration());
        d = d.minusHours(hours);
        var minutes = d.dividedBy(ChronoUnit.MINUTES.getDuration());
        d = d.minusMinutes(minutes);
        var seconds = d.dividedBy(ChronoUnit.SECONDS.getDuration());

        var sb = new StringBuilder();
        if (duration.isNegative()) sb.append("-(");

        var as = append(sb, weeks, "week", false);
        as = append(sb, days, "day", as);
        as = append(sb, hours, "hour", as);
        as = append(sb, minutes, "minute", as);
        append(sb, seconds, "second", as);

        if (duration.isNegative()) sb.append(')');

        return sb.toString();
    }

    private static boolean append(StringBuilder sb, long amt, String display, boolean addSpace) {
        amt = Math.abs(amt);
        if (amt != 0L) {
            if (addSpace) {
                sb.append(' ');
            }
            sb.append(amt).append(' ').append(display);
            if (amt != 1L) sb.append('s');
            return true;
        }
        return addSpace;
    }

    public static Duration parse(String input) throws IllegalArgumentException {
        var len = input.length();
        var total = Duration.ZERO;

        var loaded = false;
        Expect expect = Expect.EXPECT_SIGN_OR_AMT;
        int sign = 1;
        var amt = 0L;

        for (int idx = 0; idx < len; idx++) {
            var c = input.charAt(idx);
            if (c == ' ' && expect.skipSpace) continue;
            while (true) {
                switch (expect) {
                    case EXPECT_SIGN_OR_AMT -> {
                        if (c == '-') {
                            sign = -1;
                            expect = Expect.EXPECT_AMT;
                        } else if (c == '+') {
                            sign = 1;
                            expect = Expect.EXPECT_AMT;
                        } else {
                            expect = Expect.EXPECT_AMT;
                            continue;
                        }
                    }
                    case EXPECT_AMT -> {
                        if (c >= '0' && c <= '9') {
                            amt = c - '0';
                            expect = Expect.EXPECT_AMT_OR_TYPE;
                        } else {
                            throw new IllegalArgumentException("Expected amount [0-9]+, but found " + c);
                        }
                    }
                    case EXPECT_AMT_OR_TYPE -> {
                        if (c >= '0' && c <= '9') {
                            if (amt > 100_000_000_000L)
                                throw new IllegalArgumentException("Too large an amount... What are you doing???");
                            amt *= 10;
                            amt += c - '0';
                        } else {
                            expect = Expect.EXPECT_TYPE;
                            continue;
                        }
                    }
                    case EXPECT_TYPE -> {
                        var unit = switch (c) {
                            case 'w' -> ChronoUnit.WEEKS;
                            case 'd' -> ChronoUnit.DAYS;
                            case 'h' -> ChronoUnit.HOURS;
                            case 'm' -> ChronoUnit.MINUTES;
                            case 's' -> ChronoUnit.SECONDS;
                            default ->
                                    throw new IllegalArgumentException("Expected time type (s|m|h|d|w), but found " + c);
                        };
                        var duration = unit.getDuration().multipliedBy(sign * amt);
                        total = total.plus(duration);
                        expect = Expect.EXPECT_SIGN_OR_AMT;
                        loaded = true;
                    }
                }
                break;
            }
        }
        if (!loaded) {
            throw new IllegalArgumentException("Expected some input");
        }
        return total;
    }

    private enum Expect {
        EXPECT_SIGN_OR_AMT(true), EXPECT_AMT(false), EXPECT_AMT_OR_TYPE(false), EXPECT_TYPE(false);

        private final boolean skipSpace;

        Expect(boolean allowSpace) {
            this.skipSpace = allowSpace;
        }
    }
}
