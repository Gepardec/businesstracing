package fixtures.calendar;

import at.gepardec.fachtracing.api.FachTracing;

import java.time.LocalDate;
import java.time.YearMonth;

public final class CalendarPolicy {
    @FachTracing("date falls inside month")
    public boolean fallsInside(LocalDate date, YearMonth month) {
        LocalDate firstDay = month.atDay(1);
        LocalDate lastDay = month.atEndOfMonth();
        boolean afterStart = !date.isBefore(firstDay);
        boolean beforeEnd = !date.isAfter(lastDay);
        return afterStart && beforeEnd;
    }
}
