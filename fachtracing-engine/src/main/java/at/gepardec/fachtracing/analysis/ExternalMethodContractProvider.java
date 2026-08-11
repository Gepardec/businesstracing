package at.gepardec.fachtracing.analysis;

import java.util.Collection;

/** Supplies trusted semantic facts for exact source-unavailable methods. */
public interface ExternalMethodContractProvider {
    /** Stable provider identity used in deterministic conflict diagnostics. */
    String providerId();

    /** Exact method contracts supplied by this provider. */
    Collection<ExternalMethodContract> contracts();
}
