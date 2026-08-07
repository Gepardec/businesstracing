package example.application;

@GenerateDecision
public final class DecisionRequest {
    private final int age;

    public DecisionRequest(int age) {
        this.age = age;
    }

    public int age() {
        return age;
    }
}
