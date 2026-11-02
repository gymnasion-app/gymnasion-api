package com.gymnasion.tcc.event;

/**
 * Evento disparado quando um link de convite é gerado para envio por e-mail (US-03).
 */
public record ConviteAlunoCriadoEvent(
        String emailAluno,
        String nomePersonal,
        String nomeModalidade,
        String linkCadastro
) {}
