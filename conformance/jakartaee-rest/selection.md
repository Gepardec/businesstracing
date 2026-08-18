# Corpus selection

- Repository: `https://github.com/hantsy/jakartaee-rest-sample`
- Reviewed commit: `85da1d6861fea14579b1c6eb76253f0549a8e80f`
- Graph root: `com.example.interfaces.task.TaskResources#allTasks`

The root injects `TaskRepository`. The source set contains the `@ApplicationScoped`
`JpaTaskRepository` implementation. This makes it a useful unchanged CDI conformance corpus.
