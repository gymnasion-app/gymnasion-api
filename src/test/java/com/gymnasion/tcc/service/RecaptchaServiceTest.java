package com.gymnasion.tcc.service;

import com.gymnasion.tcc.exceptions.CaptchaInvalidoException;
import com.gymnasion.tcc.infra.config.RecaptchaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RecaptchaServiceTest {

    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    private RecaptchaProperties properties;
    private MockRestServiceServer google;

    @BeforeEach
    void setUp() {
        properties = new RecaptchaProperties();
        properties.setSecret("segredo-teste");
    }

    private RecaptchaService criarService() {
        RestClient.Builder builder = RestClient.builder();
        google = MockRestServiceServer.bindTo(builder).build();
        return new RecaptchaService(properties, new ObjectMapper(), builder);
    }

    @Test
    @DisplayName("US-02 C1: token aceito pelo Google libera o login")
    void US02_cenario1_tokenValido() {
        RecaptchaService service = criarService();
        google.expect(requestTo(VERIFY_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("secret=segredo-teste")))
                .andExpect(content().string(containsString("response=token-ok")))
                .andRespond(withSuccess("{\"success\": true, \"hostname\": \"localhost\"}", MediaType.APPLICATION_JSON));

        assertThatCode(() -> service.verificar("token-ok")).doesNotThrowAnyException();
        google.verify();
    }

    @Test
    @DisplayName("US-02 C5: token recusado pelo Google bloqueia a requisição")
    void US02_cenario5_tokenRecusado() {
        RecaptchaService service = criarService();
        google.expect(requestTo(VERIFY_URL))
                .andRespond(withSuccess("{\"success\": false, \"error-codes\": [\"timeout-or-duplicate\"]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.verificar("token-expirado"))
                .isInstanceOf(CaptchaInvalidoException.class)
                .hasMessage(RecaptchaService.MSG_CAPTCHA_INVALIDO);
    }

    @Test
    @DisplayName("US-02 C5: CAPTCHA não concluído (token vazio) não consulta o Google")
    void US02_cenario5_tokenAusente() {
        RecaptchaService service = criarService();

        assertThatThrownBy(() -> service.verificar("  "))
                .isInstanceOf(CaptchaInvalidoException.class)
                .hasMessage(RecaptchaService.MSG_CAPTCHA_INVALIDO);
        google.verify();
    }

    @Test
    @DisplayName("Falha de comunicação com o Google recusa a requisição (fail-closed)")
    void falhaComunicacaoRecusa() {
        RecaptchaService service = criarService();
        google.expect(requestTo(VERIFY_URL)).andRespond(withServerError());

        assertThatThrownBy(() -> service.verificar("token"))
                .isInstanceOf(CaptchaInvalidoException.class)
                .hasMessage(RecaptchaService.MSG_CAPTCHA_INDISPONIVEL);
    }

    @Test
    @DisplayName("Sem RECAPTCHA_SECRET configurado a requisição é recusada")
    void semSecretRecusa() {
        properties.setSecret("");
        RecaptchaService service = criarService();

        assertThatThrownBy(() -> service.verificar("token"))
                .isInstanceOf(CaptchaInvalidoException.class)
                .hasMessage(RecaptchaService.MSG_CAPTCHA_INDISPONIVEL);
        google.verify();
    }
}
