package com.gymnasion.tcc.event;

import com.gymnasion.tcc.service.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConviteEmailListenerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ConviteEmailListener listener;

    @Test
    @DisplayName("US-03: Listener envia e-mail com parâmetros corretos quando evento possui e-mail válido")
    void onConviteCriado_comEmailValido_chamaEmailService() {
        ConviteAlunoCriadoEvent event = new ConviteAlunoCriadoEvent(
                "aluno@teste.com",
                "Personal Lucas",
                "Musculação",
                "http://localhost:5173/cadastro-aluno?token=xyz"
        );

        listener.onConviteCriado(event);

        verify(emailService).enviarConviteAluno(
                "aluno@teste.com",
                "Personal Lucas",
                "Musculação",
                "http://localhost:5173/cadastro-aluno?token=xyz"
        );
    }

    @Test
    @DisplayName("US-03: Listener ignora evento com e-mail nulo ou em branco")
    void onConviteCriado_comEmailNuloOuBranco_ignoraEnvio() {
        listener.onConviteCriado(new ConviteAlunoCriadoEvent(null, "Personal", "Musculação", "url"));
        listener.onConviteCriado(new ConviteAlunoCriadoEvent("   ", "Personal", "Musculação", "url"));

        verifyNoInteractions(emailService);
    }
}
