package com.gymnasion.tcc.controller;

import com.gymnasion.tcc.domain.Aluno;
import com.gymnasion.tcc.domain.Usuario;
import com.gymnasion.tcc.domain.enums.Role;
import com.gymnasion.tcc.domain.enums.StatusConvite;
import com.gymnasion.tcc.dto.StravaAtividadeDTO;
import com.gymnasion.tcc.exceptions.StravaRateLimitException;
import com.gymnasion.tcc.infra.security.SecurityConfig;
import com.gymnasion.tcc.infra.security.TokenService;
import com.gymnasion.tcc.repository.AlunoRepository;
import com.gymnasion.tcc.repository.UsuarioRepository;
import com.gymnasion.tcc.service.StravaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AlunoStravaController.class, PersonalStravaController.class})
@Import(SecurityConfig.class)
class StravaControllersTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private StravaService stravaService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private AlunoRepository alunoRepository;

    private Usuario autenticarAluno() {
        Usuario alunoUser = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Aluno")
                .email("aluno@teste.com")
                .role(Role.ALUNO)
                .build();
        Aluno aluno = Aluno.builder()
                .id(UUID.randomUUID())
                .usuario(alunoUser)
                .status(StatusConvite.ATIVO)
                .build();
        when(tokenService.validateToken("token-aluno")).thenReturn("aluno@teste.com");
        when(usuarioRepository.findByEmail("aluno@teste.com")).thenReturn(Optional.of(alunoUser));
        when(alunoRepository.findByUsuario(alunoUser)).thenReturn(Optional.of(aluno));
        return alunoUser;
    }

    private Usuario autenticarPersonal() {
        Usuario personalUser = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Personal")
                .email("personal@teste.com")
                .role(Role.PERSONAL_TRAINER)
                .build();
        when(tokenService.validateToken("token-personal")).thenReturn("personal@teste.com");
        when(usuarioRepository.findByEmail("personal@teste.com")).thenReturn(Optional.of(personalUser));
        return personalUser;
    }

    private StravaAtividadeDTO atividadeExemplo() {
        return new StravaAtividadeDTO(
                "123",
                "Corrida",
                "Run",
                "Corrida",
                LocalDate.of(2026, 10, 21),
                OffsetDateTime.of(2026, 10, 21, 7, 0, 0, 0, ZoneOffset.ofHours(-3)),
                5000.0,
                5.0,
                "5,00 km",
                1500,
                "25:00",
                25,
                155,
                172,
                "05:00",
                "26:00",
                174.0,
                250.0,
                380.0,
                60.0,
                12.0,
                15.0,
                420.0
        );
    }

    @Test
    @DisplayName("Aluno: listar atividades sem limite usa defaultValue = 10")
    void aluno_listarAtividades_usaLimitePadrao10() throws Exception {
        Usuario alunoUser = autenticarAluno();
        when(stravaService.listarAtividadesRecentes(alunoUser, 10)).thenReturn(List.of(atividadeExemplo()));

        mvc.perform(get("/alunos/strava/atividades")
                        .header("Authorization", "Bearer token-aluno"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("123"))
                .andExpect(jsonPath("$[0].calorias").value(420.0));

        verify(stravaService).listarAtividadesRecentes(alunoUser, 10);
    }

    @Test
    @DisplayName("Aluno: obter detalhe de atividade por ID e mapear rate limit para HTTP 429")
    void aluno_obterDetalheAtividade_e_rateLimit429() throws Exception {
        Usuario alunoUser = autenticarAluno();
        when(stravaService.obterDetalheAtividade(alunoUser, "123")).thenReturn(atividadeExemplo());

        mvc.perform(get("/alunos/strava/atividades/123")
                        .header("Authorization", "Bearer token-aluno"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("123"))
                .andExpect(jsonPath("$.calorias").value(420.0));

        when(stravaService.obterDetalheAtividade(alunoUser, "999"))
                .thenThrow(new StravaRateLimitException("Limite temporário de requisições do Strava atingido."));

        mvc.perform(get("/alunos/strava/atividades/999")
                        .header("Authorization", "Bearer token-aluno"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.title").value("Limite de Requisições do Strava Atingido"))
                .andExpect(jsonPath("$.detail").value("Limite temporário de requisições do Strava atingido."));
    }

    @Test
    @DisplayName("Personal: listar atividades usa defaultValue = 10 e detalhar atividade retorna 200")
    void personal_listarEDetalharAtividade() throws Exception {
        Usuario personalUser = autenticarPersonal();
        UUID alunoId = UUID.randomUUID();
        when(stravaService.listarAtividadesPorPersonal(alunoId, personalUser.getId(), 10))
                .thenReturn(List.of(atividadeExemplo()));
        when(stravaService.obterDetalheAtividadePorPersonal(alunoId, personalUser.getId(), "123"))
                .thenReturn(atividadeExemplo());

        mvc.perform(get("/personal-trainers/alunos/" + alunoId + "/strava/atividades")
                        .header("Authorization", "Bearer token-personal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("123"));

        mvc.perform(get("/personal-trainers/alunos/" + alunoId + "/strava/atividades/123")
                        .header("Authorization", "Bearer token-personal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("123"))
                .andExpect(jsonPath("$.calorias").value(420.0));

        verify(stravaService).listarAtividadesPorPersonal(alunoId, personalUser.getId(), 10);
    }

    @Test
    @DisplayName("Segurança: bloqueia acesso cruzado entre perfis")
    void controleAcessoPorPerfil() throws Exception {
        autenticarAluno();
        autenticarPersonal();
        UUID alunoId = UUID.randomUUID();

        mvc.perform(get("/alunos/strava/atividades/123")
                        .header("Authorization", "Bearer token-personal"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/personal-trainers/alunos/" + alunoId + "/strava/atividades/123")
                        .header("Authorization", "Bearer token-aluno"))
                .andExpect(status().isForbidden());
    }
}
