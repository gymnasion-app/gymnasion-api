package com.gymnasion.tcc.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Contato de interesse enviado pelo formulário da landing page. */
@Entity
@Table(name = "lead")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @Column(length = 50)
    private String modalidade;

    @Builder.Default
    @Column(name = "aceita_novidades", nullable = false)
    private Boolean aceitaNovidades = false;

    @Builder.Default
    @Column(name = "data_criacao", nullable = false)
    private OffsetDateTime dataCriacao = OffsetDateTime.now();

    @Builder.Default
    @Column(name = "data_atualizacao", nullable = false)
    private OffsetDateTime dataAtualizacao = OffsetDateTime.now();
}
