package at.gepardec.fachtracing.maven;

import at.gepardec.fachtracing.analysis.BusinessEntryPoint;

import java.util.List;

/** Maven XML values for one exact business graph root. */
public final class BusinessEntryPointConfiguration {
    private String owner;
    private String method;
    private List<String> parameterTypes;
    private String label;

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public List<String> getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(List<String> parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    /** Converts Maven values to the validated engine contract. */
    BusinessEntryPoint selection() {
        return new BusinessEntryPoint(
                owner, method, parameterTypes == null ? List.of() : parameterTypes, label);
    }
}
