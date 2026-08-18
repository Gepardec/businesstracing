package at.gepardec.fachtracing.jakartaee;

import at.gepardec.fachtracing.analysis.ExternalMethodContract;
import at.gepardec.fachtracing.analysis.ExternalMethodContractProvider;
import at.gepardec.fachtracing.analysis.ExternalMethodReference;

import java.util.ArrayList;
import java.util.List;

/** Exact, application-neutral contracts for supported Jakarta EE and gRPC APIs. */
public final class JakartaEeMethodContractProvider implements ExternalMethodContractProvider {
    private static final List<ExternalMethodContract> CONTRACTS = buildContracts();
    @Override public String providerId() { return "jakartaee:platform"; }
    @Override public List<ExternalMethodContract> contracts() { return CONTRACTS; }

    private static List<ExternalMethodContract> buildContracts() {
        var values = new ArrayList<ExternalMethodContract>();
        action(values, "jakarta.persistence.EntityManager", "persist", "(Ljava/lang/Object;)V", "store entity");
        action(values, "jakarta.persistence.EntityManager", "merge", "(Ljava/lang/Object;)Ljava/lang/Object;", "store entity");
        action(values, "jakarta.persistence.EntityManager", "remove", "(Ljava/lang/Object;)V", "remove entity");
        read(values, "jakarta.persistence.EntityManager", "find", "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/Object;", "find entity");
        read(values, "jakarta.persistence.Query", "getResultList", "()Ljava/util/List;", "query results");
        read(values, "jakarta.persistence.Query", "getSingleResult", "()Ljava/lang/Object;", "query result");
        action(values, "jakarta.transaction.UserTransaction", "begin", "()V", "start transaction");
        action(values, "jakarta.transaction.UserTransaction", "commit", "()V", "commit transaction");
        action(values, "jakarta.transaction.UserTransaction", "rollback", "()V", "roll back transaction");
        read(values, "jakarta.ws.rs.core.Response", "ok", "(Ljava/lang/Object;)Ljakarta/ws/rs/core/Response$ResponseBuilder;", "create successful response");
        read(values, "jakarta.ws.rs.core.Response$ResponseBuilder", "build", "()Ljakarta/ws/rs/core/Response;", "build response");
        read(values, "jakarta.xml.ws.Service", "create", "(Ljava/net/URL;Ljavax/xml/namespace/QName;)Ljakarta/xml/ws/Service;", "create SOAP service");
        read(values, "jakarta.xml.ws.Service", "getPort", "(Ljava/lang/Class;)Ljava/lang/Object;", "get SOAP port");
        action(values, "io.grpc.ManagedChannel", "shutdown", "()Lio/grpc/ManagedChannel;", "shut down gRPC channel");
        action(values, "io.grpc.ManagedChannel", "shutdownNow", "()Lio/grpc/ManagedChannel;", "shut down gRPC channel");
        read(values, "io.grpc.ManagedChannel", "awaitTermination", "(JLjava/util/concurrent/TimeUnit;)Z", "gRPC channel terminated");
        read(values, "io.grpc.ManagedChannelBuilder", "forAddress", "(Ljava/lang/String;I)Lio/grpc/ManagedChannelBuilder;", "create gRPC channel");
        return List.copyOf(values);
    }
    private static void action(List<ExternalMethodContract> values, String owner, String name, String descriptor, String label) { values.add(new ExternalMethodContract(new ExternalMethodReference(owner, name, descriptor), ExternalMethodContract.OperationKind.ACTION, label, descriptor.endsWith("V") ? ExternalMethodContract.ResultBehavior.NONE : ExternalMethodContract.ResultBehavior.VALUE, ExternalMethodContract.StateEffect.MUTATE, java.util.Map.of(), java.util.Set.of())); }
    private static void read(List<ExternalMethodContract> values, String owner, String name, String descriptor, String label) { values.add(ExternalMethodContract.read(new ExternalMethodReference(owner, name, descriptor), label, ExternalMethodContract.ResultBehavior.VALUE)); }
}
