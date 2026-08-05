#!/usr/bin/env sh
set -eu

mvn -q -pl fachtracing-storage-jdbc -am test-compile
CP="fachtracing-api/target/classes:fachtracing-engine/target/classes:fachtracing-storage-jdbc/target/classes:fachtracing-storage-jdbc/target/test-classes:$HOME/.m2/repository/org/postgresql/postgresql/42.7.13/postgresql-42.7.13.jar"
JAVA21=${JAVA21:-$(command -v java)}
"$JAVA21" -ea -cp "$CP" at.gepardec.fachtracing.storage.jdbc.PostgresDecisionRecordRepositoryIT
