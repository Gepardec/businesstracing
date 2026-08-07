package fixtures.crossapp.scheduling;

import at.gepardec.fachtracing.api.FachTracing;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public final class ScheduleApplication {
    @FachTracing("schedule boundary")
    public Date boundary(int hour) {
        GregorianCalendar gc = new GregorianCalendar();
        gc.set(Calendar.HOUR_OF_DAY, hour);
        return gc.getTime();
    }
}
