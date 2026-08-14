package boundary;

import java.util.stream.Stream;

/** Binary-only rules used to prove source-boundary behavior. */
public final class SourceBoundaryBinaryRules {
    private SourceBoundaryBinaryRules() { }

    public interface Rule {
        boolean approve(String value);
    }

    public interface State {
        void update();
        boolean approved();
    }

    public interface StreamSource {
        Stream<String> values();
    }

    public static final class Nested {
        private Nested() { }

        public static boolean accepts(int age) {
            return age >= 18;
        }

        public static Object find(String value) {
            return value.isBlank() ? null : value;
        }

        public static Object read(String value) {
            if (value.isBlank()) throw new IllegalArgumentException("blank");
            return value;
        }
    }
}
