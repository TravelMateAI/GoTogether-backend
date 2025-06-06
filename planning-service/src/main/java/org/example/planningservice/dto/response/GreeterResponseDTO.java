package org.example.planningservice.dto.response;

public class GreeterResponseDTO {
    private String message;

    public GreeterResponseDTO() {
    }

    public GreeterResponseDTO(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
