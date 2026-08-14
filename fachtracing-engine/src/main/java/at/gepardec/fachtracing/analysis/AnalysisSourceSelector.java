package at.gepardec.fachtracing.analysis;

import at.gepardec.fachtracing.api.FachTracing;

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

/** Selects the entry and resolution inputs for one project analysis. */
final class AnalysisSourceSelector {
    private AnalysisSourceSelector() { }

    /** Selects one analysis request, or no request when the project has no graph entry source. */
    @FachTracing("select source inputs for graph analysis")
    static Optional<Selection> select(
            ApplicationSourceBoundary boundary,
            ApplicationSourceBoundary.ProjectSources project) {
        Objects.requireNonNull(boundary, "boundary");
        Objects.requireNonNull(project, "project");
        if (project.entrySourceFiles().isEmpty()) return Optional.empty();

        List<ApplicationSourceBoundary.ProjectSources> closure = projectClosure(boundary, project);
        boolean modular = project.moduleDescriptor().isPresent();
        List<ApplicationSourceBoundary.ProjectSources> sourceProjects = modular
                ? closure.stream().filter(item -> item.moduleDescriptor().isPresent()).toList()
                : closure;
        List<Path> projectResolutionSources = sourceProjects.stream()
                .flatMap(item -> item.resolutionSourceFiles().stream()).toList();
        List<Path> externalResolutionSources = boundary.externalResolutionSources().stream()
                .map(ApplicationSourceBoundary.ResolutionSource::path).toList();
        List<Path> analysisSources = java.util.stream.Stream.concat(
                        projectResolutionSources.stream(), externalResolutionSources.stream())
                .distinct().sorted(Comparator.comparing(Path::toString)).toList();
        List<Path> connectedClasspath = closure.stream()
                .flatMap(item -> item.compilationClasspath().stream()).distinct()
                .sorted(Comparator.comparing(Path::toString)).toList();
        List<Path> entrySources = project.entrySourceFiles();
        var request = new AnalysisRequest(
                analysisSources, connectedClasspath, project.compilerModel().charset(), entrySources);
        return Optional.of(new Selection(request, sourceProjects, modular));
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
