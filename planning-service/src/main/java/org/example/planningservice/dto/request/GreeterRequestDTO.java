package org.example.planningservice.dto.request;

public class GreeterRequestDTO {
    private String name;

    public GreeterRequestDTO() {
    }

    public GreeterRequestDTO(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
