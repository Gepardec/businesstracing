/** Optional Spring method semantics for Fachtracing analysis. */
module at.gepardec.fachtracing.spring {
    requires at.gepardec.fachtracing.engine;

    exports at.gepardec.fachtracing.spring;

    provides at.gepardec.fachtracing.analysis.ExternalMethodContractProvider
            with at.gepardec.fachtracing.spring.SpringMethodContractProvider;
}
