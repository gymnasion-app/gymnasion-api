package com.gymnasion.tcc.controller;

import com.gymnasion.tcc.dto.LeadRequestDTO;
import com.gymnasion.tcc.service.LeadService;
import com.gymnasion.tcc.service.RecaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/leads")
@AllArgsConstructor
@Tag(name = "Leads", description = "Contatos de interesse da landing page pública")
public class LeadController {

    private final LeadService leadService;
    private final RecaptchaService recaptchaService;

    @PostMapping
    @Operation(summary = "Registrar interesse (landing page)", description = "Endpoint público protegido por reCAPTCHA v2. Um e-mail já registrado tem seus dados atualizados.")
    @ApiResponse(responseCode = "201", description = "Interesse registrado.")
    @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos ou CAPTCHA inválido.")
    public ResponseEntity<Void> registrar(@Valid @RequestBody LeadRequestDTO dto) {
        recaptchaService.verificar(dto.captchaToken());
        leadService.registrar(dto);
        return ResponseEntity.status(201).build();
    }
}
