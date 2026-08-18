/** Optional Jakarta EE semantics for Fachtracing analysis. */
module at.gepardec.fachtracing.jakartaee {
    requires at.gepardec.fachtracing.engine;
    requires java.compiler;
    provides at.gepardec.fachtracing.analysis.ExternalMethodContractProvider
            with at.gepardec.fachtracing.jakartaee.JakartaEeMethodContractProvider;
    provides at.gepardec.fachtracing.analysis.DynamicDispatchTargetSelector
            with at.gepardec.fachtracing.jakartaee.CdiDispatchTargetSelector;
    exports at.gepardec.fachtracing.jakartaee;
}
