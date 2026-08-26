package at.gepardec.fachtracing.model;

/** Internal semantic evidence that the source analyzer attaches to exact graph nodes. */
public final class BusinessSemanticAttributes {
    public static final String OWNER_TYPE = "semantic.ownerType";
    public static final String ENCLOSING_METHOD = "semantic.enclosingMethod";
    public static final String TREE_KIND = "semantic.treeKind";
    public static final String CALL_METHOD = "semantic.callMethod";
    public static final String CALL_OWNER_TYPE = "semantic.callOwnerType";
    public static final String CALL_RETURN_TYPE = "semantic.callReturnType";
    public static final String RECEIVER = "semantic.receiver";
    public static final String ARGUMENTS = "semantic.arguments";
    public static final String ARGUMENT_TYPES = "semantic.argumentTypes";
    public static final String NEGATED = "semantic.negated";
    public static final String STATEMENT_CALL = "semantic.statementCall";
    public static final String CONTEXT_SUBJECT = "semantic.contextSubject";
    public static final String AGGREGATE_SCOPE = "semantic.aggregateScope";
    public static final String AGGREGATE_ITEM = "semantic.aggregateItem";
    public static final String PARENT_AGGREGATE_SCOPE = "semantic.parentAggregateScope";
    public static final String ROLE = "semantic.role";

    public static final String IMPLEMENTATION = "implementation";
    public static final String AGGREGATE = "aggregate";

    private BusinessSemanticAttributes() { }
}
