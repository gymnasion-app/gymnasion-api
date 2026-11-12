package com.gymnasion.tcc.repository;

import com.gymnasion.tcc.domain.Aluno;
import com.gymnasion.tcc.domain.SessaoTreino;
import com.gymnasion.tcc.domain.enums.StatusSessaoTreino;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessaoTreinoRepository extends JpaRepository<SessaoTreino, UUID> {

    @Query("SELECT DISTINCT s FROM SessaoTreino s LEFT JOIN FETCH s.metricas WHERE s.rotinaTreino.id = :rotinaId ORDER BY s.semana ASC, s.dataPlanejada ASC")
    List<SessaoTreino> findByRotinaTreinoIdOrderBySemanaAscDataPlanejadaAsc(@Param("rotinaId") UUID rotinaTreinoId);

    @Query("SELECT DISTINCT s FROM SessaoTreino s LEFT JOIN FETCH s.metricas WHERE s.rotinaTreino.id = :rotinaId AND s.semana = :semana ORDER BY s.dataPlanejada ASC")
    List<SessaoTreino> findByRotinaTreinoIdAndSemanaOrderByDataPlanejadaAsc(@Param("rotinaId") UUID rotinaTreinoId, @Param("semana") Integer semana);

    @Query("SELECT DISTINCT s FROM SessaoTreino s LEFT JOIN FETCH s.metricas WHERE s.rotinaTreino.id = :rotinaId AND s.status = :status ORDER BY s.dataReal DESC")
    List<SessaoTreino> findByRotinaTreinoIdAndStatusOrderByDataRealDesc(@Param("rotinaId") UUID rotinaTreinoId, @Param("status") StatusSessaoTreino status);

    Page<SessaoTreino> findByRotinaTreinoIdAndStatusOrderByDataRealDesc(UUID rotinaTreinoId, StatusSessaoTreino status, Pageable pageable);

    @Query("SELECT DISTINCT s FROM SessaoTreino s " +
            "LEFT JOIN FETCH s.metricas " +
            "JOIN s.rotinaTreino r " +
            "WHERE r.personalTrainer.id = :personalId AND s.id = :sessaoId")
    Optional<SessaoTreino> findByIdAndPersonalTrainerId(@Param("sessaoId") UUID sessaoId, @Param("personalId") UUID personalId);

    @Query("SELECT DISTINCT s FROM SessaoTreino s " +
            "LEFT JOIN FETCH s.metricas " +
            "JOIN s.rotinaTreino r " +
            "JOIN r.alunos a " +
            "WHERE a.id = :alunoId AND s.id = :sessaoId")
    Optional<SessaoTreino> findByIdAndAlunoId(@Param("sessaoId") UUID sessaoId, @Param("alunoId") UUID alunoId);

    @Query("SELECT DISTINCT s FROM SessaoTreino s " +
            "LEFT JOIN FETCH s.metricas " +
            "JOIN s.rotinaTreino r " +
            "JOIN r.alunos a " +
            "WHERE a.id = :alunoId AND s.status = :status " +
            "ORDER BY s.dataReal DESC")
    List<SessaoTreino> findByAlunoIdAndStatusOrderByDataRealDesc(@Param("alunoId") UUID alunoId, @Param("status") StatusSessaoTreino status);

    @Query(value = "SELECT s FROM SessaoTreino s " +
            "JOIN s.rotinaTreino r " +
            "JOIN r.alunos a " +
            "WHERE a.id = :alunoId AND s.status = :status " +
            "ORDER BY s.dataReal DESC",
            countQuery = "SELECT count(s) FROM SessaoTreino s JOIN s.rotinaTreino r JOIN r.alunos a WHERE a.id = :alunoId AND s.status = :status")
    Page<SessaoTreino> findByAlunoIdAndStatusOrderByDataRealDesc(@Param("alunoId") UUID alunoId, @Param("status") StatusSessaoTreino status, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM SessaoTreino s JOIN s.metricas m WHERE m.id = :metricaId AND s.status = :status")
    boolean existsByMetricaIdAndStatus(@Param("metricaId") UUID metricaId, @Param("status") StatusSessaoTreino status);

    @Query("SELECT DISTINCT a FROM SessaoTreino s JOIN s.metricas m JOIN s.rotinaTreino r JOIN r.alunos a WHERE m.id = :metricaId AND s.status = :status")
    List<Aluno> findAlunosByMetricaIdAndStatus(@Param("metricaId") UUID metricaId, @Param("status") StatusSessaoTreino status);

    @Query("SELECT DISTINCT a FROM SessaoTreino s JOIN s.metricas m JOIN s.rotinaTreino r JOIN r.alunos a WHERE m.id = :metricaId")
    List<Aluno> findAlunosByMetricaId(@Param("metricaId") UUID metricaId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM sessao_treino_metrica stm " +
            "USING sessao_treino st " +
            "WHERE stm.sessao_treino_id = st.id " +
            "AND stm.metrica_id = :metricaId " +
            "AND st.status = 'PLANEJADA'", nativeQuery = true)
    void removerMetricaDeSessoesPlanejadas(@Param("metricaId") UUID metricaId);
}
