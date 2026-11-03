package com.gymnasion.tcc.event;

import com.gymnasion.tcc.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envia o e-mail de convite ao aluno (US-03) de forma assíncrona após o commit
 * da transação que persistiu o convite, evitando retenção de conexões no HikariCP.
 */
@Component
@RequiredArgsConstructor
public class ConviteEmailListener {

    private final EmailService emailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConviteCriado(ConviteAlunoCriadoEvent event) {
        if (event.emailAluno() == null || event.emailAluno().isBlank()) {
            return;
        }
        emailService.enviarConviteAluno(
                event.emailAluno(),
                event.nomePersonal(),
                event.nomeModalidade(),
                event.linkCadastro()
        );
    }
}
