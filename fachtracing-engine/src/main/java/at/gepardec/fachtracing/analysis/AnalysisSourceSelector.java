package at.gepardec.fachtracing.analysis;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/** Selects the entry, source, and classpath inputs for one project analysis. */
final class AnalysisSourceSelector {
    private AnalysisSourceSelector() { }

    /** Selects one request, or no request when the project has no graph entry source. */
    static Optional<Selection> select(
            ApplicationSourceBoundary boundary,
            ApplicationSourceBoundary.ProjectSources project,
            List<ExternalMethodContractProvider> contractProviders,
            List<BusinessEntryPoint> businessEntryPoints) {
        Objects.requireNonNull(boundary, "boundary");
        Objects.requireNonNull(project, "project");
        contractProviders = List.copyOf(Objects.requireNonNull(contractProviders, "contractProviders"));
        businessEntryPoints = List.copyOf(Objects.requireNonNull(businessEntryPoints, "businessEntryPoints"));

        SourceSelectionPlan plan = selectPlan(
                !project.entrySourceFiles().isEmpty(), project.moduleDescriptor().isPresent());
        if (plan == SourceSelectionPlan.SKIP_PROJECT_WITH_NO_ENTRY_SOURCE) return Optional.empty();

        List<ApplicationSourceBoundary.ProjectSources> closure = projectClosure(boundary, project);
        boolean modular = plan == SourceSelectionPlan.USE_MODULAR_PROJECT_SOURCES_WITH_EXTERNAL_SOURCES_CLASSPATH_AND_ENTRIES;
        List<ApplicationSourceBoundary.ProjectSources> sourceProjects = modular
                ? closure.stream().filter(item -> item.moduleDescriptor().isPresent()).toList()
                : closure;
        List<Path> projectSources = sourceProjects.stream()
                .flatMap(item -> item.resolutionSourceFiles().stream()).toList();
        List<Path> externalSources = boundary.externalResolutionSources().stream()
                .map(ApplicationSourceBoundary.ResolutionSource::path).toList();
        List<Path> analysisSources = java.util.stream.Stream.concat(
                        projectSources.stream(), externalSources.stream())
                .distinct().sorted(Comparator.comparing(Path::toString)).toList();
        List<Path> connectedClasspath = closure.stream()
                .flatMap(item -> item.compilationClasspath().stream()).distinct()
                .sorted(Comparator.comparing(Path::toString)).toList();
        var request = new AnalysisRequest(
                analysisSources, connectedClasspath, project.compilerModel().charset(),
                project.entrySourceFiles())
                .withExternalMethodContractProviders(contractProviders)
                .withBusinessEntryPoints(businessEntryPoints);
        return Optional.of(new Selection(request, sourceProjects, modular));
    }

    /** Selects the source roles that one project analysis must use. */
    static SourceSelectionPlan selectPlan(boolean hasGraphEntrySource, boolean modularProject) {
        if (!hasGraphEntrySource) return SourceSelectionPlan.SKIP_PROJECT_WITH_NO_ENTRY_SOURCE;
        if (modularProject) {
            return SourceSelectionPlan.USE_MODULAR_PROJECT_SOURCES_WITH_EXTERNAL_SOURCES_CLASSPATH_AND_ENTRIES;
        }
        return SourceSelectionPlan.USE_CONNECTED_PROJECT_SOURCES_WITH_EXTERNAL_SOURCES_CLASSPATH_AND_ENTRIES;
    }

    private static List<ApplicationSourceBoundary.ProjectSources> projectClosure(
            ApplicationSourceBoundary boundary,
            ApplicationSourceBoundary.ProjectSources root) {
        Map<String, ApplicationSourceBoundary.ProjectSources> projects = boundary.projects().stream()
                .collect(Collectors.toMap(ApplicationSourceBoundary.ProjectSources::projectId,
                        project -> project, (left, right) -> left, LinkedHashMap::new));
        var pending = new ArrayDeque<String>();
        var selected = new LinkedHashSet<String>();
        pending.add(root.projectId());
        while (!pending.isEmpty()) {
            String id = pending.removeFirst();
            if (!selected.add(id)) continue;
            pending.addAll(projects.get(id).projectDependencies());
            projects.values().stream()
                    .filter(candidate -> candidate.projectDependencies().contains(id))
                    .map(ApplicationSourceBoundary.ProjectSources::projectId)
                    .forEach(pending::addLast);
        }
        return selected.stream().map(projects::get).toList();
    }

    /** The source roles for one analysis request. */
    enum SourceSelectionPlan {
        SKIP_PROJECT_WITH_NO_ENTRY_SOURCE,
        USE_CONNECTED_PROJECT_SOURCES_WITH_EXTERNAL_SOURCES_CLASSPATH_AND_ENTRIES,
        USE_MODULAR_PROJECT_SOURCES_WITH_EXTERNAL_SOURCES_CLASSPATH_AND_ENTRIES
    }

    /** Immutable source selection consumed by flat or modular graph analysis. */
    record Selection(
            AnalysisRequest request,
            List<ApplicationSourceBoundary.ProjectSources> sourceProjects,
            boolean modular) {
        Selection {
            request = Objects.requireNonNull(request, "request");
            sourceProjects = List.copyOf(Objects.requireNonNull(sourceProjects, "sourceProjects"));
            if (sourceProjects.isEmpty()) {
                throw new IllegalArgumentException("at least one source project is required");
            }
        }
    }
}
