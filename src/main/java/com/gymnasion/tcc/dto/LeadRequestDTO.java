package com.gymnasion.tcc.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LeadRequestDTO(
    @NotBlank(message = "Por favor, informe seu nome.")
    @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
    String nome,

    @NotBlank(message = "Por favor, insira um e-mail válido.")
    @Email(message = "Por favor, insira um e-mail válido.")
    @Size(max = 100, message = "O e-mail deve ter no máximo 100 caracteres.")
    String email,

    @Size(max = 50, message = "Modalidade inválida.")
    String modalidade,

    Boolean aceitaNovidades,

    @NotBlank(message = "Conclua a verificação CAPTCHA.")
    String captchaToken
) {}
