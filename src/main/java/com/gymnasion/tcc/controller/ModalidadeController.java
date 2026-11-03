package com.gymnasion.tcc.controller;

import com.gymnasion.tcc.dto.ModalidadeRequestDTO;
import com.gymnasion.tcc.dto.ModalidadeResponseDTO;
import com.gymnasion.tcc.dto.PageResponseDTO;
import com.gymnasion.tcc.service.ModalidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/modalidades")
@AllArgsConstructor
@Tag(name = "Modalidades Esportivas", description = "Consulta e gestão do catálogo de modalidades desportivas do sistema")
public class ModalidadeController {

    private final ModalidadeService modalidadeService;

    @GetMapping
    @Operation(summary = "Listar modalidades", description = "Retorna a lista de modalidades desportivas disponíveis de forma paginada. Acesso livre.")
    public ResponseEntity<PageResponseDTO<ModalidadeResponseDTO>> listarTodas(
            @ParameterObject @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(value = "apenasAtivas", required = false, defaultValue = "true") boolean apenasAtivas) {
        return ResponseEntity.ok(PageResponseDTO.from(modalidadeService.listarTodas(pageable, apenasAtivas)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar modalidade por ID", description = "Retorna os detalhes de uma modalidade específica. Acesso livre.")
    public ResponseEntity<ModalidadeResponseDTO> buscarPorId(@PathVariable("id") Long id) {
        return ResponseEntity.ok(modalidadeService.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Criar nova modalidade (Admin)", description = "Adiciona uma nova modalidade desportiva ao sistema. Restrito a administradores.")
    public ResponseEntity<ModalidadeResponseDTO> criar(@Valid @RequestBody ModalidadeRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(modalidadeService.criar(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualizar modalidade (Admin)", description = "Atualiza nome e descrição de uma modalidade. Restrito a administradores.")
    public ResponseEntity<ModalidadeResponseDTO> atualizar(
            @PathVariable("id") Long id,
            @Valid @RequestBody ModalidadeRequestDTO dto) {
        return ResponseEntity.ok(modalidadeService.atualizar(id, dto));
    }

    @PatchMapping("/{id}/desativar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desativar modalidade (Admin)", description = "Desabilita uma modalidade desportiva para novos usos. Restrito a administradores.")
    public ResponseEntity<ModalidadeResponseDTO> desativar(@PathVariable("id") Long id) {
        return ResponseEntity.ok(modalidadeService.desativar(id));
    }

    @PatchMapping("/{id}/ativar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reativar modalidade (Admin)", description = "Reabilita uma modalidade desportiva previamente desativada. Restrito a administradores.")
    public ResponseEntity<ModalidadeResponseDTO> ativar(@PathVariable("id") Long id) {
        return ResponseEntity.ok(modalidadeService.ativar(id));
    }
}