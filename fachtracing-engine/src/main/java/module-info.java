/** Core Fachtracing graph, execution, explanation, and storage module. */
module at.gepardec.fachtracing.engine {
    requires at.gepardec.fachtracing.api;
    requires jdk.compiler;
    requires org.objectweb.asm;
    requires org.objectweb.asm.tree;

    exports at.gepardec.fachtracing;
    exports at.gepardec.fachtracing.model;
    exports at.gepardec.fachtracing.analysis;
    exports at.gepardec.fachtracing.store;
    exports at.gepardec.fachtracing.runtime;
    exports at.gepardec.fachtracing.explain;
    exports at.gepardec.fachtracing.plantuml;
    exports at.gepardec.fachtracing.mermaid;
    exports at.gepardec.fachtracing.developer;
    exports at.gepardec.fachtracing.business;
}
