package itmo.blps.util;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

public final class WeeklyPeriodUtil {

    private WeeklyPeriodUtil() {
    }

    public record Period(Instant start, Instant end) {
    }

    public static Period previousCalendarWeek(Instant now) {
        ZonedDateTime z = now.atZone(ZoneOffset.UTC);
        ZonedDateTime thisMonday = z.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .toLocalDate()
                .atStartOfDay(ZoneOffset.UTC);
        ZonedDateTime prevMonday = thisMonday.minusWeeks(1);
        Instant periodStart = prevMonday.toInstant();
        Instant periodEnd = thisMonday.toInstant().minusNanos(1);
        return new Period(periodStart, periodEnd);
    }
}
