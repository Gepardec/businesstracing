# Performance verification

The corrected required load comparison passed on 2026-08-04 using OpenJDK 21.0.2 on macOS 15.7.5
(Apple Silicon). The harness used its documented 10 ms representative application workload,
a 60-second instrumented-disabled baseline and an instrumented-enabled phase at 1,000
completed invocations per second for 600 seconds. Ten adjacent baseline/enabled windows distribute
the baseline across the run to remove phase-order and machine-state drift; the reported percentiles
are calculated from the combined raw samples, not averages of window percentiles.

| Metric | Result |
| --- | ---: |
| Enabled completed invocations | 600,000 |
| Baseline p50 | 11,509.500 µs |
| Baseline p95 | 12,530.000 µs |
| Enabled p50 | 11,521.334 µs |
| Enabled p95 | 12,538.875 µs |
| Enabled p95 overhead | 0.071% |
| Application errors | 0 |
| Result mismatches | 0 |
| Dropped traces | 0 |
| Cross-trace contamination | 0 |

Exact harness output:

```text
PERFORMANCE_RESULT rate=1000 baseline_seconds=60 enabled_seconds=600 baseline_p50_us=11509.500 baseline_p95_us=12530.000 enabled_p50_us=11521.334 enabled_p95_us=12538.875 p95_overhead_percent=0.071 completed=600000 errors=0 mismatches=0 dropped=0 contamination=0
```

This result characterizes the walking skeleton on the stated machine and workload; it is not a
universal latency guarantee. Applications should rerun the same harness with representative
decision latency, adapters, redaction, graph size, JVM flags, and deployment hardware.
