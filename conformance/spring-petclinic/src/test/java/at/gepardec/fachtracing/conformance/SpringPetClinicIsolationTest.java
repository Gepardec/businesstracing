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
    }

    private static void assertGeneric(Path file) {
        try {
            String content = Files.readString(file).toLowerCase();
            for (String forbidden : List.of("org.springframework.samples.petclinic", "spring-petclinic",
                    "baseentity", "petcontroller", "find an eligible pet")) {
                assert !content.contains(forbidden) : "reference-specific token in " + file + ": " + forbidden;
            }
        }
        catch (IOException error) {
            throw new IllegalStateException("Could not inspect " + file, error);
        }
    }
}
