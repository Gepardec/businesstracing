# Jakarta EE REST sample conformance

This conformance test analyzes a clean checkout of the public Jakarta EE REST sample.
It proves that the optional adapter selects the CDI-managed JPA repository at the REST endpoint.

Run `./scripts/verify-jakartaee-rest.sh`. Set `JAKARTAEE_REST_SAMPLE_DIR` when the pinned
source tree is not at `/tmp/fachtracing-jakartaee-rest-sample`.
