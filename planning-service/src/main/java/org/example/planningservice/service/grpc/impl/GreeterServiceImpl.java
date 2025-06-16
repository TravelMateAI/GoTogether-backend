package org.example.planningservice.service.grpc.impl;

import org.example.planningservice.grpc.client.GreeterClient; // Corrected import
import org.example.planningservice.dto.request.GreeterRequestDTO;
import org.example.planningservice.dto.response.GreeterResponseDTO;
import org.example.planningservice.grpc.apiservice.common.HelloReply;
import org.example.planningservice.service.grpc.GreeterService;
import org.springframework.stereotype.Service;

@Service
public class GreeterServiceImpl implements GreeterService {

    private final GreeterClient greeterClient;

    public GreeterServiceImpl(GreeterClient greeterClient) {
        this.greeterClient = greeterClient;
    }

    @Override
    public GreeterResponseDTO sayHello(GreeterRequestDTO request) {
        HelloReply reply = greeterClient.sayHello(request.getName());
        return new GreeterResponseDTO(reply.getMessage());
    }
}
