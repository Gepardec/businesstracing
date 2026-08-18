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
        read(values, "jakarta.validation.Validation", "buildDefaultValidatorFactory", "()Ljakarta/validation/ValidatorFactory;", "create validator");
        read(values, "jakarta.validation.ValidatorFactory", "getValidator", "()Ljakarta/validation/Validator;", "get validator");
        read(values, "jakarta.validation.Validator", "validate", "(Ljava/lang/Object;[Ljava/lang/Class;)Ljava/util/Set;", "validation results");
        action(values, "jakarta.transaction.UserTransaction", "begin", "()V", "start transaction");
        action(values, "jakarta.transaction.UserTransaction", "commit", "()V", "commit transaction");
        action(values, "jakarta.transaction.UserTransaction", "rollback", "()V", "roll back transaction");
        action(values, "jakarta.ejb.EJBContext", "setRollbackOnly", "()V", "mark transaction for rollback");
        read(values, "jakarta.ejb.EJBContext", "getCallerPrincipal", "()Ljava/security/Principal;", "current caller");
        predicate(values, "jakarta.ejb.EJBContext", "isCallerInRole", "(Ljava/lang/String;)Z", "caller has role");
        read(values, "jakarta.security.enterprise.SecurityContext", "getCallerPrincipal", "()Ljava/security/Principal;", "current caller");
        predicate(values, "jakarta.security.enterprise.SecurityContext", "isCallerInRole", "(Ljava/lang/String;)Z", "caller has role");
        read(values, "jakarta.ws.rs.core.Response", "ok", "(Ljava/lang/Object;)Ljakarta/ws/rs/core/Response$ResponseBuilder;", "create successful response");
        read(values, "jakarta.ws.rs.core.Response", "status", "(I)Ljakarta/ws/rs/core/Response$ResponseBuilder;", "create response");
        read(values, "jakarta.ws.rs.core.Response$ResponseBuilder", "build", "()Ljakarta/ws/rs/core/Response;", "build response");
        read(values, "jakarta.xml.ws.Service", "create", "(Ljava/net/URL;Ljavax/xml/namespace/QName;)Ljakarta/xml/ws/Service;", "create SOAP service");
        read(values, "jakarta.xml.ws.Service", "getPort", "(Ljava/lang/Class;)Ljava/lang/Object;", "get SOAP port");
        read(values, "jakarta.ws.rs.client.ClientBuilder", "newClient", "()Ljakarta/ws/rs/client/Client;", "create REST client");
        read(values, "jakarta.ws.rs.client.Client", "target", "(Ljava/lang/String;)Ljakarta/ws/rs/client/WebTarget;", "create REST target");
        read(values, "jakarta.ws.rs.client.WebTarget", "request", "()Ljakarta/ws/rs/client/Invocation$Builder;", "create REST request");
        read(values, "jakarta.jms.JMSContext", "createProducer", "()Ljakarta/jms/JMSProducer;", "create message producer");
        read(values, "jakarta.jms.JMSContext", "createConsumer", "(Ljakarta/jms/Destination;)Ljakarta/jms/JMSConsumer;", "create message consumer");
        action(values, "jakarta.jms.JMSProducer", "send", "(Ljakarta/jms/Destination;Ljava/lang/String;)Ljakarta/jms/JMSProducer;", "send message");
        read(values, "jakarta.jms.JMSConsumer", "receive", "()Ljakarta/jms/Message;", "receive message");
        read(values, "jakarta.json.bind.Jsonb", "fromJson", "(Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;", "read JSON value");
        read(values, "jakarta.json.bind.Jsonb", "toJson", "(Ljava/lang/Object;)Ljava/lang/String;", "write JSON value");
        action(values, "jakarta.mail.Transport", "send", "(Ljakarta/mail/Message;)V", "send email");
        read(values, "jakarta.servlet.ServletRequest", "getParameter", "(Ljava/lang/String;)Ljava/lang/String;", "request parameter");
        read(values, "jakarta.servlet.http.HttpServletRequest", "getSession", "()Ljakarta/servlet/http/HttpSession;", "HTTP session");
        read(values, "jakarta.websocket.Session", "getBasicRemote", "()Ljakarta/websocket/RemoteEndpoint$Basic;", "websocket endpoint");
        action(values, "jakarta.websocket.RemoteEndpoint$Basic", "sendText", "(Ljava/lang/String;)V", "send websocket message");
        action(values, "io.grpc.ManagedChannel", "shutdown", "()Lio/grpc/ManagedChannel;", "shut down gRPC channel");
        action(values, "io.grpc.ManagedChannel", "shutdownNow", "()Lio/grpc/ManagedChannel;", "shut down gRPC channel");
        read(values, "io.grpc.ManagedChannel", "awaitTermination", "(JLjava/util/concurrent/TimeUnit;)Z", "gRPC channel terminated");
        predicate(values, "io.grpc.ManagedChannel", "isShutdown", "()Z", "gRPC channel is shut down");
        predicate(values, "io.grpc.ManagedChannel", "isTerminated", "()Z", "gRPC channel is terminated");
        read(values, "io.grpc.StatusRuntimeException", "getStatus", "()Lio/grpc/Status;", "gRPC status");
        read(values, "io.grpc.ManagedChannelBuilder", "forAddress", "(Ljava/lang/String;I)Lio/grpc/ManagedChannelBuilder;", "create gRPC channel");
        read(values, "io.grpc.ManagedChannelBuilder", "usePlaintext", "()Lio/grpc/ManagedChannelBuilder;", "configure gRPC channel");
        read(values, "io.grpc.ManagedChannelBuilder", "build", "()Lio/grpc/ManagedChannel;", "build gRPC channel");
        return List.copyOf(values);
    }
    private static void action(List<ExternalMethodContract> values, String owner, String name, String descriptor, String label) { values.add(new ExternalMethodContract(new ExternalMethodReference(owner, name, descriptor), ExternalMethodContract.OperationKind.ACTION, label, descriptor.endsWith("V") ? ExternalMethodContract.ResultBehavior.NONE : ExternalMethodContract.ResultBehavior.VALUE, ExternalMethodContract.StateEffect.MUTATE, java.util.Map.of(), java.util.Set.of())); }
    private static void read(List<ExternalMethodContract> values, String owner, String name, String descriptor, String label) { values.add(ExternalMethodContract.read(new ExternalMethodReference(owner, name, descriptor), label, ExternalMethodContract.ResultBehavior.VALUE)); }
    private static void predicate(List<ExternalMethodContract> values, String owner, String name, String descriptor, String label) { values.add(ExternalMethodContract.predicate(new ExternalMethodReference(owner, name, descriptor), label)); }
}
