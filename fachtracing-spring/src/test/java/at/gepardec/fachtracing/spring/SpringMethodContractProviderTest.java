package at.gepardec.fachtracing.spring;

import at.gepardec.fachtracing.analysis.AnalysisRequest;
import at.gepardec.fachtracing.analysis.ExternalMethodContract;
import at.gepardec.fachtracing.analysis.ExternalMethodContractProviders;
import at.gepardec.fachtracing.analysis.ExternalMethodContractRegistry;
import at.gepardec.fachtracing.analysis.ExternalMethodReference;
import at.gepardec.fachtracing.analysis.StaticDecisionAnalyzer;
import at.gepardec.fachtracing.business.BusinessGraphProjector;
import at.gepardec.fachtracing.business.BusinessLogicArtifactGuard;
import at.gepardec.fachtracing.model.BusinessDecisionGraph;
import at.gepardec.fachtracing.model.BusinessLogicGraph;

import java.lang.reflect.Method;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Executable contracts for the optional Spring semantic adapter. */
public final class SpringMethodContractProviderTest {
    private static final Path FIXTURE = Path.of(
            "fachtracing-spring/src/test/resources/fixtures/spring/FrameworkWorkflow.java");
    private static final List<Path> CLASSPATH = Arrays.stream(System.getProperty("java.class.path")
                    .split(java.io.File.pathSeparator))
            .map(Path::of).toList();

    private SpringMethodContractProviderTest() { }

    public static void main(String[] args) {
        loadsTheProviderAsAService();
        matchesOnlyRealExactSpringSignatures();
        matchesDerivedPageQueriesOnlyForRepositorySubtypes();
        completesGeneralSpringWorkflows();
        keepsUnsupportedSpringCallsIncomplete();
        containsNoApplicationVocabulary();
        productionHasNoSpringLinkageOrApplicationRules();
    }

    private static void matchesDerivedPageQueriesOnlyForRepositorySubtypes() {
        var method = new ExternalMethodReference(
                "fixture.spring.FrameworkWorkflow$EntryRepository", "findByNameStartingWith",
                "(Ljava/lang/String;Lorg/springframework/data/domain/Pageable;)"
                        + "Lorg/springframework/data/domain/Page;");
        var registry = ExternalMethodContractRegistry.of(List.of(new SpringMethodContractProvider()));
        var resolved = registry.resolve(method, Set.of(
                "fixture.spring.FrameworkWorkflow$EntryRepository",
                "org.springframework.data.repository.Repository"));
        assert resolved.kind() == ExternalMethodContractRegistry.ResolutionKind.RESOLVED : resolved;
        assert resolved.contract().orElseThrow().businessLabel().equals("find matching records");
        assert resolved.contract().orElseThrow().possibleExceptionTypes()
                .equals(Set.of("org.springframework.dao.DataAccessException"));
        assert registry.resolve(method, Set.of("fixture.spring.FrameworkWorkflow$EntryRepository")).kind()
                == ExternalMethodContractRegistry.ResolutionKind.ABSENT;

        var unsupported = new ExternalMethodReference(
                method.ownerBinaryName(), "loadMatchingEntries", method.descriptor());
        assert registry.resolve(unsupported, Set.of("org.springframework.data.repository.Repository")).kind()
                == ExternalMethodContractRegistry.ResolutionKind.ABSENT;
    }

    private static void loadsTheProviderAsAService() {
        var providers = ExternalMethodContractProviders.load(
                SpringMethodContractProviderTest.class.getClassLoader());
        assert providers.stream().map(provider -> provider.providerId()).toList()
                .contains("spring:framework") : providers;
    }

    private static void matchesOnlyRealExactSpringSignatures() {
        var provider = new SpringMethodContractProvider();
        var registry = ExternalMethodContractRegistry.of(List.of(provider));
        for (ExternalMethodContract contract : provider.contracts()) {
            assert methodExists(contract.method()) : contract.method();
            assert registry.resolve(contract.method()).kind()
                    == ExternalMethodContractRegistry.ResolutionKind.RESOLVED : contract;
            for (String exceptionType : contract.possibleExceptionTypes()) {
                try {
                    Class.forName(exceptionType);
                } catch (ClassNotFoundException exception) {
                    throw new AssertionError("contract exception type is unavailable: " + exceptionType, exception);
                }
            }
        }
        assert registry.resolve(new ExternalMethodReference(
                "org.springframework.util.StringUtils", "capitalize",
                "(Ljava/lang/String;)Ljava/lang/String;")).kind()
                == ExternalMethodContractRegistry.ResolutionKind.ABSENT;
    }

    private static void completesGeneralSpringWorkflows() {
        var request = AnalysisRequest.of(List.of(FIXTURE), CLASSPATH)
                .withExternalMethodContractProviders(List.of(new SpringMethodContractProvider()));
        var results = new StaticDecisionAnalyzer().analyzeAll(request);
        for (String label : List.of(
                "classify result page", "search repository records", "submit valid record")) {
            var result = results.stream().filter(candidate -> candidate.graph().decisionLabel().equals(label))
                    .findFirst().orElseThrow();
            assert result.graph().completeness() == BusinessDecisionGraph.Completeness.COMPLETE
                    : label + ": " + result.graph().coverageGaps();
            BusinessLogicGraph business = new BusinessGraphProjector().project(result);
            assert business.completeness() == BusinessLogicGraph.Completeness.COMPLETE : business;
            new BusinessLogicArtifactGuard().requireClean(business);
        }
        String labels = results.stream().flatMap(result -> result.graph().nodes().stream())
                .map(BusinessDecisionGraph.DecisionNode::businessLabel).toList().toString();
        assert labels.contains("result page is empty") : labels;
        assert labels.contains("result count") : labels;
        assert labels.contains("text is absent") : labels;
        assert labels.contains("validation has errors") : labels;
        assert labels.contains("record field validation error") : labels;
        assert labels.contains("save record") : labels;
        assert labels.contains("add response message") : labels;
        String outcomes = results.stream().flatMap(result -> result.graph().edges().stream())
                .map(BusinessDecisionGraph.DecisionEdge::outcome).toList().toString();
        assert !outcomes.contains("unexpected failure") : outcomes;
    }

    private static void keepsUnsupportedSpringCallsIncomplete() {
        var request = AnalysisRequest.of(List.of(FIXTURE), CLASSPATH)
                .withExternalMethodContractProviders(List.of(new SpringMethodContractProvider()));
        var results = new StaticDecisionAnalyzer().analyzeAll(request);
        for (String label : List.of("unsupported Spring helper", "unsupported custom page query")) {
            var result = results.stream()
                    .filter(candidate -> candidate.graph().decisionLabel().equals(label))
                    .findFirst().orElseThrow();
            assert result.graph().completeness() == BusinessDecisionGraph.Completeness.INCOMPLETE : result;
            assert !result.graph().coverageGaps().isEmpty() : result;
        }
    }

    private static void containsNoApplicationVocabulary() {
        String catalog = new SpringMethodContractProvider().contracts().stream()
                .map(contract -> contract.method().ownerBinaryName() + ' '
                        + contract.method().methodName() + ' ' + contract.businessLabel() + ' '
                        + String.join(" ", contract.possibleExceptionTypes()))
                .collect(java.util.stream.Collectors.joining(" ")).toLowerCase(Locale.ROOT);
        for (String prohibited : List.of("petclinic", "owner", "pet", "visit", "veterinarian")) {
            assert !java.util.regex.Pattern.compile("\\b" + prohibited + "\\b").matcher(catalog).find()
                    : prohibited + " appears in " + catalog;
        }
    }

    private static void productionHasNoSpringLinkageOrApplicationRules() {
        Path main = Path.of("fachtracing-spring/src/main");
        try (var files = Files.walk(main)) {
            String source = files.filter(Files::isRegularFile)
                    .map(path -> {
                        try {
                            return Files.readString(path);
                        } catch (IOException exception) {
                            throw new java.io.UncheckedIOException(exception);
                        }
                    })
                    .collect(java.util.stream.Collectors.joining("\n"));
            assert !source.matches("(?s).*import\\s+org\\.springframework\\..*") : source;
            assert !source.matches("(?s).*requires\\s+org\\.springframework\\..*") : source;
            for (String prohibited : List.of("petclinic", "pet name", "visit date", "veterinarian")) {
                assert !source.toLowerCase(Locale.ROOT).contains(prohibited) : prohibited;
            }
        } catch (IOException exception) {
            throw new AssertionError("cannot inspect adapter production sources", exception);
        }

        try {
            String pom = Files.readString(Path.of("fachtracing-spring/pom.xml"));
            java.util.regex.Matcher springDependency = java.util.regex.Pattern.compile(
                    "(?s)<dependency>.*?<groupId>org\\.springframework(?:\\.data)?</groupId>.*?</dependency>")
                    .matcher(pom);
            int count = 0;
            while (springDependency.find()) {
                count++;
                assert springDependency.group().contains("<scope>test</scope>") : springDependency.group();
            }
            assert count >= 6 : "expected real Spring test dependencies";
        } catch (IOException exception) {
            throw new AssertionError("cannot inspect adapter POM", exception);
        }
    }

    private static boolean methodExists(ExternalMethodReference reference) {
        try {
            Class<?> owner = Class.forName(reference.ownerBinaryName());
            for (Method method : owner.getMethods()) {
                if (method.getName().equals(reference.methodName())
                        && descriptor(method).equals(reference.descriptor())) return true;
            }
            return false;
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("contract owner is unavailable: " + reference.ownerBinaryName(), exception);
        }
    }

    private static String descriptor(Method method) {
        var descriptor = new StringBuilder("(");
        for (Class<?> parameter : method.getParameterTypes()) descriptor.append(typeDescriptor(parameter));
        return descriptor.append(')').append(typeDescriptor(method.getReturnType())).toString();
    }

    private static String typeDescriptor(Class<?> type) {
        if (type.isArray()) return type.getName().replace('.', '/');
        if (!type.isPrimitive()) return "L" + type.getName().replace('.', '/') + ";";
        if (type == void.class) return "V";
        if (type == boolean.class) return "Z";
        if (type == byte.class) return "B";
        if (type == char.class) return "C";
        if (type == short.class) return "S";
        if (type == int.class) return "I";
        if (type == long.class) return "J";
        if (type == float.class) return "F";
        if (type == double.class) return "D";
        throw new IllegalArgumentException("unsupported primitive: " + type);
    }
}
