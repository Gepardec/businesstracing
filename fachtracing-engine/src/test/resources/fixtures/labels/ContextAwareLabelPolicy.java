package fixtures.labels;

import at.gepardec.fachtracing.api.FachTracing;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.GregorianCalendar;
import java.util.List;

public final class ContextAwareLabelPolicy {
    private final Deque<String> q = new ArrayDeque<>();

    @FachTracing("calendar bounds")
    public Date startOfDay(boolean today) {
        Date from = null;
        if (today) {
            from = timeAt(0, 0, 0);
        }
        return from;
    }

    private Date timeAt(int hour, int minute, int second) {
        Calendar c = new GregorianCalendar();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, second);
        return c.getTime();
    }

    @FachTracing("inferred calendar")
    public Date inferredCalendar(boolean reset) {
        var c = new GregorianCalendar();
        if (reset) {
            c.set(Calendar.HOUR_OF_DAY, 0);
        }
        return c.getTime();
    }

    @FachTracing("static sort")
    public List<String> sortWarnings(boolean required, List<String> warnings) {
        if (required) {
            Collections.sort(warnings);
        }
        return warnings;
    }

    @FachTracing("static fill")
    public int[] fillBuffer(boolean required, int[] buffer, int value) {
        if (required) {
            Arrays.fill(buffer, value);
        }
        return buffer;
    }

    @FachTracing("scoped receiver")
    public Deque<String> scopedReceiver(boolean reset) {
        if (reset) {
            Calendar q = new GregorianCalendar();
            q.set(Calendar.HOUR_OF_DAY, 0);
            if (q.getTimeInMillis() > 0) {
                this.q.clear();
            }
        }
        q.add("sensor");
        return q;
    }

    @FachTracing("abbreviated comparator")
    public int compare(boolean naturalOrder) {
        Comparator<String> comp = naturalOrder
                ? Comparator.naturalOrder()
                : Comparator.reverseOrder();
        return comp.compare("first", "second");
    }

    @FachTracing("collection add")
    public boolean addSensor(boolean required, List<String> sensors) {
        if (required) {
            sensors.add("sensor");
        }
        return sensors.isEmpty();
    }

    @FachTracing("collection add all")
    public boolean addSensors(boolean required, List<String> sensors, List<String> additions) {
        if (required) {
            sensors.addAll(additions);
        }
        return sensors.isEmpty();
    }

    @FachTracing("generic collection name")
    public boolean hasMappedSensor(boolean required) {
        List<SensorData> list = new java.util.ArrayList<>();
        if (required) {
            list.add(new SensorData("sensor"));
        }
        return list.isEmpty();
    }

    private record SensorData(String name) { }
}
