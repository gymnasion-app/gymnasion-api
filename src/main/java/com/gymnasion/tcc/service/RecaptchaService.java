package com.gymnasion.tcc.service;

import com.gymnasion.tcc.exceptions.CaptchaInvalidoException;
import com.gymnasion.tcc.infra.config.RecaptchaProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

/**
 * Valida no Google o token gerado pelo widget reCAPTCHA v2 "Não sou um robô" (US-01, US-02 C1/C5).
 * Cada token só pode ser verificado uma vez e expira em cerca de 2 minutos.
 */
@Slf4j
@Service
public class RecaptchaService {

    public static final String MSG_CAPTCHA_INVALIDO = "Verificação CAPTCHA inválida. Tente novamente.";
    public static final String MSG_CAPTCHA_INDISPONIVEL = "Não foi possível validar o CAPTCHA no momento. Tente novamente.";

    private final RecaptchaProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public RecaptchaService(RecaptchaProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, RestClient.builder().requestFactory(criarRequestFactory()));
    }

    RecaptchaService(RecaptchaProperties properties, ObjectMapper objectMapper, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.restClient = restClientBuilder.build();

        if (!properties.isConfigured()) {
            log.warn("reCAPTCHA não configurado: defina RECAPTCHA_SECRET. Login web e cadastro de personal serão recusados.");
        }
    }

    private static SimpleClientHttpRequestFactory criarRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return factory;
    }

    public void verificar(String token) {
        if (token == null || token.isBlank()) {
            throw new CaptchaInvalidoException(MSG_CAPTCHA_INVALIDO);
        }
        if (!properties.isConfigured()) {
            log.error("Tentativa de verificação de CAPTCHA sem RECAPTCHA_SECRET configurado.");
            throw new CaptchaInvalidoException(MSG_CAPTCHA_INDISPONIVEL);
        }

        JsonNode resposta;
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("secret", properties.getSecret());
            form.add("response", token);

            String body = restClient.post()
                    .uri(properties.getVerifyUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            resposta = objectMapper.readTree(body);
        } catch (Exception e) {
            log.error("Falha ao comunicar com o Google reCAPTCHA", e);
            throw new CaptchaInvalidoException(MSG_CAPTCHA_INDISPONIVEL);
        }

        if (!resposta.path("success").asBoolean(false)) {
            log.info("CAPTCHA recusado pelo Google: {}", resposta.path("error-codes"));
            throw new CaptchaInvalidoException(MSG_CAPTCHA_INVALIDO);
        }
    }
}
