package at.gepardec.fachtracing.conformance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Keeps Spring PetClinic corpus knowledge out of production and generic configuration. */
public final class SpringPetClinicIsolationTest {
    private SpringPetClinicIsolationTest() { }

    public static void main(String[] args) throws IOException {
        Path root = Path.of(args.length == 0 ? "." : args[0]).toAbsolutePath().normalize();
        List<Path> guarded = List.of(
                root.resolve("fachtracing-api/src/main"),
                root.resolve("fachtracing-engine/src/main"),
                root.resolve("fachtracing-spring/src/main"),
                root.resolve("fachtracing-agent/src/main"),
                root.resolve("fachtracing-maven-plugin/src/main"),
                root.resolve("fachtracing-storage-jdbc/src/main"),
                root.resolve("pom.xml"));
        for (Path path : guarded) {
            if (Files.isDirectory(path)) {
                try (var files = Files.walk(path)) {
                    files.filter(Files::isRegularFile).forEach(SpringPetClinicIsolationTest::assertGeneric);
                }
            }
            else {
                assertGeneric(path);
            }
        }
        assertAnnotationOnlyOverlay(root.resolve("conformance/spring-petclinic/annotation-overlay.patch"));
    }

    private static void assertAnnotationOnlyOverlay(Path overlay) throws IOException {
        List<String> changes = Files.readAllLines(overlay).stream()
                .filter(line -> (line.startsWith("+") && !line.startsWith("+++"))
                        || (line.startsWith("-") && !line.startsWith("---")))
                .toList();
        assert changes.size() == 6 : "overlay has non-annotation changes: " + changes;
        long imports = changes.stream().filter(line -> line.equals(
                "+import at.gepardec.fachtracing.api.FachTracing;")).count();
        long annotations = changes.stream().filter(line -> line.matches(
                "\\+\\s*@FachTracing\\(\"(?:owner search|visit booking|pet registration)\"\\)"))
                .count();
        assert imports == 3 : "overlay must add one import per source file: " + changes;
        assert annotations == 3 : "overlay must add only the three reviewed annotations: " + changes;
        assert changes.stream().noneMatch(line -> line.startsWith("-"))
                : "overlay must not remove application source: " + changes;
    }

    private static void assertGeneric(Path file) {
        try {
            String content = Files.readString(file).toLowerCase();
            for (String forbidden : List.of("org.springframework.samples.petclinic", "spring-petclinic",
                    "baseentity", "petcontroller", "visitcontroller", "ownercontroller",
                    "find an eligible pet", "unique_owner_pet_name",
                    "findbylastnamestartingwith", "processnewvisitform", "processcreationform")) {
                assert !content.contains(forbidden) : "reference-specific token in " + file + ": " + forbidden;
            }
        }
        catch (IOException error) {
            throw new IllegalStateException("Could not inspect " + file, error);
        }
    }
}
