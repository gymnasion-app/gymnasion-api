package com.gymnasion.tcc.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do Google reCAPTCHA v2 (US-01 e US-02).
 * A chave secreta vem da variável de ambiente RECAPTCHA_SECRET e nunca é exposta ao front-end.
 */
@Configuration
@ConfigurationProperties(prefix = "recaptcha")
@Getter
@Setter
public class RecaptchaProperties {

    private String secret;
    private String verifyUrl = "https://www.google.com/recaptcha/api/siteverify";

    public boolean isConfigured() {
        return secret != null && !secret.isBlank();
    }
}
