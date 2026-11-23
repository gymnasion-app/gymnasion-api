package com.gymnasion.tcc.repository;

import com.gymnasion.tcc.domain.AuditoriaDesempenho;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface AuditoriaDesempenhoRepository extends JpaRepository<AuditoriaDesempenho, UUID> {
    List<AuditoriaDesempenho> findByDesempenhoIdOrderByDataHoraAsc(UUID desempenhoId);

    Page<AuditoriaDesempenho> findByDesempenhoIdOrderByDataHoraAsc(UUID desempenhoId, Pageable pageable);

    boolean existsByMetricaId(UUID metricaId);

    @Query("SELECT DISTINCT a.metrica.id FROM AuditoriaDesempenho a WHERE a.metrica.id IN :metricaIds")
    Set<UUID> findMetricaIdsWithAuditoria(@Param("metricaIds") Collection<UUID> metricaIds);
}
