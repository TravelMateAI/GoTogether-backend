package org.example.planningservice.service.grpc;

import org.example.planningservice.dto.request.GreeterRequestDTO;
import org.example.planningservice.dto.response.GreeterResponseDTO;

public interface GreeterService {
    GreeterResponseDTO sayHello(GreeterRequestDTO request);
}
