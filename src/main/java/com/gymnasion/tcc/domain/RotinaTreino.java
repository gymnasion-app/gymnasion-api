package com.gymnasion.tcc.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "rotina_treino")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RotinaTreino {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String objetivos;

    @Column(name = "frequencia_semanal", nullable = false)
    private Integer frequenciaSemanal;

    @Builder.Default
    @Column(nullable = false)
    private Boolean ativo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modalidade_id", nullable = false)
    private Modalidade modalidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personal_trainer_id", nullable = false)
    private PersonalTrainer personalTrainer;

    @ManyToMany
    @JoinTable(
            name = "segue",
            joinColumns = @JoinColumn(name = "rotina_treino_id"),
            inverseJoinColumns = @JoinColumn(name = "aluno_id")
    )
    @Builder.Default
    private Set<Aluno> alunos = new HashSet<>();

    @OneToMany(mappedBy = "rotinaTreino", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<SessaoTreino> sessoes = new java.util.ArrayList<>();

    /**
     * Auxiliar em memória para derivar as métricas ativas distintas vinculadas às sessões
     * quando a coleção de sessões já estiver inicializada no contexto de persistência.
     */
    public List<Metrica> getMetricasUtilizadasNasSessoes() {
        if (this.sessoes == null || this.sessoes.isEmpty()) {
            return List.of();
        }
        return this.sessoes.stream()
                .map(SessaoTreino::getMetricas)
                .filter(Objects::nonNull)
                .flatMap(Set::stream)
                .filter(m -> Boolean.TRUE.equals(m.getAtivo()))
                .distinct()
                .sorted(Comparator.comparing(Metrica::getTitulo, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}