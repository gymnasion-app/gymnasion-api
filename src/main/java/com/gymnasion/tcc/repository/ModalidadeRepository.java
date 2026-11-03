package com.gymnasion.tcc.repository;

import com.gymnasion.tcc.domain.Modalidade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModalidadeRepository extends JpaRepository<Modalidade, Long> {
    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

    List<Modalidade> findByAtivoTrue();

    Page<Modalidade> findByAtivoTrue(Pageable pageable);
}
