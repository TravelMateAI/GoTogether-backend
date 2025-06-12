import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import route.DirectionsServiceGrpc;
import route.Route.RouteRequest;
import route.Route.RouteResponse;

import java.io.IOException;

public class MockRouteServer {

    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = ServerBuilder.forPort(9090)
                .addService(new DirectionsServiceGrpc.DirectionsServiceImplBase() {
                    @Override
                    public void getRoute(RouteRequest request, StreamObserver<RouteResponse> responseObserver) {
                        String origin = request.getOrigin();
                        String destination = request.getDestination();

                        // Mocked response
                        RouteResponse response = RouteResponse.newBuilder()
                                .setPolyline("mock_encoded_polyline_string")
                                .setDistance("145 km")
                                .setDuration("3 hours 12 minutes")
                                .build();

                        responseObserver.onNext(response);
                        responseObserver.onCompleted();
                    }
                })
                .build();

        server.start();
        System.out.println("✅ Mock DirectionsService gRPC server started on port 9090");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("🛑 Shutting down mock gRPC server...");
            server.shutdown();
        }));

        server.awaitTermination();
    }
}
