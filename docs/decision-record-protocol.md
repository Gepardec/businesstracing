# Decision record protocol

`fachtracing-decision-record/v1` is deterministic UTF-8 JSON. A reader rejects another schema ID
and ignores unknown fields in a V1 document. This permits additive fields without changing the
meaning of existing fields.

The envelope contains a stable record ID, execution and graph IDs, graph version, source-boundary
fingerprint, UTC start and completion times, terminal status, typed final result, ordered node and
selected-edge evidence, completeness, coverage gaps, redacted correlation values, and a non-secret
redaction-policy ID. A failed execution has generic failure data and no final value. Technical
exception, Java, Maven, source, and credential data is forbidden.

Correlation values must be redacted or keyed-hashed before envelope construction. The repository
receives only the resulting typed value. It can query an inclusive completion-time range and the
redacted canonical value. Retention deletes records completed strictly before the supplied boundary.

## Delivery

`DecisionRecordDelivery` uses a bounded queue and one daemon storage worker. `offer` performs no
repository I/O. The default integration policy should be `FAIL_OPEN`; a full queue preserves the
application decision, rejects no application call, and increments `dropped`. `REJECT_NEW_TRACE`
returns false and increments `rejected`. `BLOCK` is opt-in for deployments that accept application
backpressure.

Repository failures retry only on the worker with a configured retry count and delay. Counters report
accepted, saved, retried, rejected, and dropped records. Shutdown drains accepted work for up to ten
seconds and counts any remainder as dropped. Production deployments must alert on rejected, dropped,
and repeated retry counts. Redaction, retention, deletion authorization, and backup deletion remain
deployment responsibilities.
