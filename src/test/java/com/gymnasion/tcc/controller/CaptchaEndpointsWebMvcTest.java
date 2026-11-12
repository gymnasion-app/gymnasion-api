package com.gymnasion.tcc.controller;

import com.gymnasion.tcc.domain.enums.Role;
import com.gymnasion.tcc.dto.LeadRequestDTO;
import com.gymnasion.tcc.dto.LoginRequestDTO;
import com.gymnasion.tcc.dto.RegistroPersonalCompletoDTO;
import com.gymnasion.tcc.dto.TokenResponseDTO;
import com.gymnasion.tcc.exceptions.CaptchaInvalidoException;
import com.gymnasion.tcc.infra.security.SecurityConfig;
import com.gymnasion.tcc.infra.security.TokenService;
import com.gymnasion.tcc.repository.AlunoRepository;
import com.gymnasion.tcc.repository.UsuarioRepository;
import com.gymnasion.tcc.service.AuthService;
import com.gymnasion.tcc.service.LeadService;
import com.gymnasion.tcc.service.PersonalConviteService;
import com.gymnasion.tcc.service.RecaptchaService;
import com.gymnasion.tcc.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Valida, na camada HTTP e com a configuração real de segurança, a proteção por reCAPTCHA v2
 * do login web, do cadastro de personal (US-01, US-02) e do formulário de leads da landing page.
 */
@WebMvcTest(controllers = {AuthController.class, LeadController.class})
@Import(SecurityConfig.class)
class CaptchaEndpointsWebMvcTest {

    private static final String MSG_CAPTCHA = "Verificação CAPTCHA inválida. Tente novamente.";

    @Autowired private MockMvc mvc;

    @MockitoBean private RecaptchaService recaptchaService;
    @MockitoBean private AuthService authService;
    @MockitoBean private UsuarioService usuarioService;
    @MockitoBean private PersonalConviteService conviteService;
    @MockitoBean private LeadService leadService;
    // dependências do SecurityFilter real
    @MockitoBean private TokenService tokenService;
    @MockitoBean private UsuarioRepository usuarioRepository;
    @MockitoBean private AlunoRepository alunoRepository;

    private static final String LOGIN_WEB = """
            {"email":"personal@exemplo.com","password":"Senha@123","captchaToken":"token-ok"}""";
    private static final String REGISTRO = """
            {"nome":"Ana Souza","email":"ana@exemplo.com","password":"Forte@2026","cpf":"12345678900",
             "celular":"11999999999","modalidade":1,"aceitouTermos":true,"captchaToken":"token-ok"}""";
    private static final String LEAD = """
            {"nome":"Ana Souza","email":"ana@exemplo.com","modalidade":"natacao","aceitaNovidades":true,"captchaToken":"token-ok"}""";

    private void captchaRecusado() {
        doThrow(new CaptchaInvalidoException(MSG_CAPTCHA)).when(recaptchaService).verificar(any());
    }

    // ===== POST /auth/login-web =====

    @Test
    @DisplayName("US-02 C1: CAPTCHA válido é verificado antes das credenciais e o login retorna o token")
    void US02_cenario1_captchaValidoAutentica() throws Exception {
        when(authService.login(any())).thenReturn(TokenResponseDTO.of("jwt", UUID.randomUUID(), Role.PERSONAL_TRAINER));

        mvc.perform(post("/auth/login-web").contentType(MediaType.APPLICATION_JSON).content(LOGIN_WEB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt"))
                .andExpect(jsonPath("$.role").value("PERSONAL_TRAINER"));

        InOrder ordem = inOrder(recaptchaService, authService);
        ordem.verify(recaptchaService).verificar("token-ok");
        ArgumentCaptor<LoginRequestDTO> login = ArgumentCaptor.forClass(LoginRequestDTO.class);
        ordem.verify(authService).login(login.capture());
        assertThat(login.getValue().email()).isEqualTo("personal@exemplo.com");
        assertThat(login.getValue().password()).isEqualTo("Senha@123");
    }

    @Test
    @DisplayName("US-02 C5: CAPTCHA recusado retorna 400 sem consultar credenciais (não conta tentativa RN-010)")
    void US02_cenario5_captchaRecusado() throws Exception {
        captchaRecusado();

        mvc.perform(post("/auth/login-web").contentType(MediaType.APPLICATION_JSON).content(LOGIN_WEB))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("CAPTCHA Inválido"))
                .andExpect(jsonPath("$.detail").value(MSG_CAPTCHA));

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("US-02 C5: CAPTCHA não concluído (sem token) é barrado na validação")
    void US02_cenario5_semToken() throws Exception {
        mvc.perform(post("/auth/login-web").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"personal@exemplo.com\",\"password\":\"Senha@123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidFields.captchaToken").value("Conclua a verificação CAPTCHA."));

        verifyNoInteractions(recaptchaService, authService);
    }

    @Test
    @DisplayName("Endpoint é público (não exige JWT)")
    void endpointPublico() throws Exception {
        when(authService.login(any())).thenReturn(TokenResponseDTO.of("jwt", UUID.randomUUID(), Role.PERSONAL_TRAINER));
        mvc.perform(post("/auth/login-web").contentType(MediaType.APPLICATION_JSON).content(LOGIN_WEB))
                .andExpect(status().isOk());
    }

    // ===== POST /auth/login (app mobile) =====

    @Test
    @DisplayName("Continua funcionando sem CAPTCHA para não quebrar o app mobile")
    void loginMobileSemCaptcha() throws Exception {
        when(authService.login(any())).thenReturn(TokenResponseDTO.of("jwt", UUID.randomUUID(), Role.ALUNO));

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"aluno@exemplo.com\",\"password\":\"Senha@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt"));

        verifyNoInteractions(recaptchaService);
    }

    // ===== POST /auth/registro-personal =====

    @Test
    @DisplayName("US-01: CAPTCHA válido é verificado antes de criar a conta")
    void US01_captchaValidoCriaConta() throws Exception {
        mvc.perform(post("/auth/registro-personal").contentType(MediaType.APPLICATION_JSON).content(REGISTRO))
                .andExpect(status().isCreated());

        InOrder ordem = inOrder(recaptchaService, usuarioService);
        ordem.verify(recaptchaService).verificar("token-ok");
        ArgumentCaptor<RegistroPersonalCompletoDTO> dto = ArgumentCaptor.forClass(RegistroPersonalCompletoDTO.class);
        ordem.verify(usuarioService).criarPersonal(dto.capture());
        assertThat(dto.getValue().captchaToken()).isEqualTo("token-ok");
        assertThat(dto.getValue().aceitouTermos()).isTrue();
    }

    @Test
    @DisplayName("US-01: CAPTCHA recusado retorna 400 e nenhuma conta é criada")
    void US01_captchaRecusado() throws Exception {
        captchaRecusado();

        mvc.perform(post("/auth/registro-personal").contentType(MediaType.APPLICATION_JSON).content(REGISTRO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(MSG_CAPTCHA));

        verifyNoInteractions(usuarioService);
    }

    @Test
    @DisplayName("US-01: cadastro sem token é barrado na validação")
    void US01_semToken() throws Exception {
        mvc.perform(post("/auth/registro-personal").contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO.replace(",\"captchaToken\":\"token-ok\"", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidFields.captchaToken").value("Conclua a verificação CAPTCHA."));

        verifyNoInteractions(recaptchaService, usuarioService);
    }

    // ===== POST /leads (landing page) =====

    @Test
    @DisplayName("Lead com CAPTCHA válido é registrado (201) e o endpoint é público")
    void leadValidoRegistrado() throws Exception {
        mvc.perform(post("/leads").contentType(MediaType.APPLICATION_JSON).content(LEAD))
                .andExpect(status().isCreated());

        InOrder ordem = inOrder(recaptchaService, leadService);
        ordem.verify(recaptchaService).verificar("token-ok");
        ArgumentCaptor<LeadRequestDTO> dto = ArgumentCaptor.forClass(LeadRequestDTO.class);
        ordem.verify(leadService).registrar(dto.capture());
        assertThat(dto.getValue().nome()).isEqualTo("Ana Souza");
        assertThat(dto.getValue().modalidade()).isEqualTo("natacao");
        assertThat(dto.getValue().aceitaNovidades()).isTrue();
    }

    @Test
    @DisplayName("Bot com token recusado não grava lead")
    void captchaRecusadoNaoGrava() throws Exception {
        captchaRecusado();

        mvc.perform(post("/leads").contentType(MediaType.APPLICATION_JSON).content(LEAD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(MSG_CAPTCHA));

        verifyNoInteractions(leadService);
    }

    @Test
    @DisplayName("Bot sem token não grava lead")
    void semTokenNaoGrava() throws Exception {
        mvc.perform(post("/leads").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Bot\",\"email\":\"bot@exemplo.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidFields.captchaToken").value("Conclua a verificação CAPTCHA."));

        verifyNoInteractions(recaptchaService, leadService);
    }

    @Test
    @DisplayName("Nome vazio e e-mail inválido retornam as mensagens exibidas no formulário")
    void camposInvalidos() throws Exception {
        mvc.perform(post("/leads").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"email\":\"invalido\",\"captchaToken\":\"token-ok\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidFields.nome").value("Por favor, informe seu nome."))
                .andExpect(jsonPath("$.invalidFields.email").value("Por favor, insira um e-mail válido."));

        verifyNoInteractions(leadService);
    }

    @Test
    @DisplayName("Somente o POST é público: listar leads exige autenticação")
    void listagemNaoPublica() throws Exception {
        int status = mvc.perform(get("/leads")).andReturn().getResponse().getStatus();
        assertThat(status).isIn(401, 403);
    }
}
