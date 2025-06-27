package com.ISA.Restaurant.Dto.Request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyEmailDto {
    private String email;
    private String verificationCode;
}
