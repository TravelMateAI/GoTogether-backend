package com.ISA.Restaurant.service;

 import customer_restaurant.grpc.GiveMeTrueRequest;
 import customer_restaurant.grpc.GiveMeTrueResponse;
 import customer_restaurant.grpc.Message;
 import customer_restaurant.grpc.MessageServiceGrpc;
 import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class MessageServiceImpl extends MessageServiceGrpc.MessageServiceImplBase {

    @Override
    public void send(Message request, StreamObserver<Message> responseObserver) {
        // create response
        Message message = Message.newBuilder()
                .setMessage("Jayanthi received your message from kotte: Thank U ❤️ \n Ur message is: " + request.getMessage())
                .build();

        // add to stream observer
        responseObserver.onNext(message);

        // complete
        responseObserver.onCompleted();
    }

    @Override
    public void giveMeTrue(GiveMeTrueRequest request, StreamObserver<GiveMeTrueResponse> responseObserver) {
        responseObserver.onNext(GiveMeTrueResponse.newBuilder().setTrue(true).build());
        responseObserver.onCompleted();
    }
}