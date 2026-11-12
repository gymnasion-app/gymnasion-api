package com.gymnasion.tcc.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Login da área web do personal, protegido por reCAPTCHA v2 (US-02). */
public record LoginWebRequestDTO(
    @NotBlank(message = "O email é obrigatório.")
    @Email(message = "E-mail inválido.")
    String email,

    @NotBlank(message = "A senha é obrigatória.")
    String password,

    @NotBlank(message = "Conclua a verificação CAPTCHA.")
    String captchaToken
) {
    public LoginRequestDTO toLoginRequest() {
        return new LoginRequestDTO(email, password);
    }
}
