/** JDBC persistence adapter for Fachtracing decision envelopes. */
module at.gepardec.fachtracing.storage.jdbc {
    requires at.gepardec.fachtracing.engine;
    requires java.sql;
    requires static java.naming;

    exports at.gepardec.fachtracing.storage.jdbc;
}
