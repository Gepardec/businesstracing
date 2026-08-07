package example.processor;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

@SupportedAnnotationTypes("example.application.GenerateDecision")
@SupportedOptions("example.option")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public final class DecisionProcessor extends AbstractProcessor {
    private boolean generated;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        if (generated || annotations.isEmpty() || roundEnvironment.processingOver()) return false;
        generated = true;
        try {
            JavaFileObject source = processingEnv.getFiler()
                    .createSourceFile("example.generated.GeneratedApprovalPolicy");
            try (Writer writer = source.openWriter()) {
                writer.write("""
                        package example.generated;

                        import at.gepardec.fachtracing.api.FachTracing;
                        import example.application.DecisionRequest;
                        import javax.annotation.processing.Generated;

                        @Generated("example.processor.DecisionProcessor")
                        public final class GeneratedApprovalPolicy {
                            @FachTracing("generated approval")
                            public String approve(DecisionRequest request) {
                                if (request.age() >= 18) return "approved";
                                return "manual review";
                            }
                        }
                        """);
            }
        } catch (IOException error) {
            throw new IllegalStateException("Could not generate the decision source", error);
        }
        return true;
    }
}
