package com.gymnasion.tcc.service;

import com.gymnasion.tcc.domain.Metrica;
import com.gymnasion.tcc.domain.enums.CampoStrava;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Validação e normalização canônica dos valores de métricas conforme o tipo e unidade (RN-015 / US-18).
 * Utilizada no registro da execução (US-22) e na correção de desempenho (US-23).
 */
public final class ValidadorValorMetrica {

    private static final Pattern FORMATO_TEMPO_RITMO = Pattern.compile("^\\d{1,2}:[0-5]\\d$");
    private static final Pattern FORMATO_TEMPO_MMSS = Pattern.compile("^[0-5]?\\d:[0-5]\\d$");
    private static final Pattern FORMATO_TEMPO_HHMMSS = Pattern.compile("^\\d{1,2}:[0-5]\\d:[0-5]\\d$");
    private static final int FREQUENCIA_CARDIACA_MINIMA = 30;
    private static final int FREQUENCIA_CARDIACA_MAXIMA = 250;
    private static final int TAMANHO_MAXIMO_VALOR_METRICA = 255;

    private ValidadorValorMetrica() {
    }

    public record ResultadoValidacao(boolean valido, String valorNormalizado, String mensagemErro) {
        public static ResultadoValidacao ok(String valorNormalizado) {
            return new ResultadoValidacao(true, valorNormalizado, null);
        }

        public static ResultadoValidacao erro(String mensagemErro) {
            return new ResultadoValidacao(false, null, mensagemErro);
        }
    }

    /**
     * Valida e normaliza o valor informado para formato canônico antes da persistência ou auditoria.
     */
    public static ResultadoValidacao validarENormalizar(Metrica metrica, String valor) {
        String titulo = metrica.getTitulo();
        if (valor == null || valor.isBlank()) {
            return ResultadoValidacao.erro(
                    "Valor inválido para a métrica '" + titulo + "'. Verifique o formato esperado: o valor não pode estar em branco.");
        }

        String trimmed = valor.trim();
        if (trimmed.length() > TAMANHO_MAXIMO_VALOR_METRICA) {
            return ResultadoValidacao.erro(
                    "Valor inválido para a métrica '" + titulo + "'. Verifique o formato esperado: o valor deve ter no máximo "
                            + TAMANHO_MAXIMO_VALOR_METRICA + " caracteres.");
        }

        return switch (metrica.getTipo()) {
            case NUMERICO -> {
                String normalizado = trimmed.replace(',', '.');
                try {
                    new BigDecimal(normalizado);
                    yield ResultadoValidacao.ok(normalizado);
                } catch (NumberFormatException e) {
                    yield ResultadoValidacao.erro(
                            "Valor inválido para a métrica '" + titulo + "'. Verifique o formato esperado: deve ser numérico.");
                }
            }
            case TEMPO -> validarENormalizarTempo(metrica, titulo, trimmed);
            case FREQUENCIA_CARDIACA -> {
                try {
                    int bpm = Integer.parseInt(trimmed);
                    if (bpm >= FREQUENCIA_CARDIACA_MINIMA && bpm <= FREQUENCIA_CARDIACA_MAXIMA) {
                        yield ResultadoValidacao.ok(String.valueOf(bpm));
                    }
                    yield ResultadoValidacao.erro(
                            "Valor inválido para a métrica '" + titulo + "'. Verifique o formato esperado: deve estar entre "
                                    + FREQUENCIA_CARDIACA_MINIMA + " e " + FREQUENCIA_CARDIACA_MAXIMA + " bpm.");
                } catch (NumberFormatException e) {
                    yield ResultadoValidacao.erro(
                            "Valor inválido para a métrica '" + titulo
                                    + "'. Verifique o formato esperado: deve ser um número inteiro de batimentos por minuto (bpm).");
                }
            }
            case TEXTO -> ResultadoValidacao.ok(trimmed);
        };
    }

    private static ResultadoValidacao validarENormalizarTempo(Metrica metrica, String titulo, String trimmed) {
        boolean isRitmo = CampoStrava.isUnidadeRitmo(metrica.getUnidade())
                || (metrica.getUnidade() == null && metrica.getCampoStrava() == CampoStrava.RITMO_MEDIO);

        if (isRitmo) {
            if (!FORMATO_TEMPO_RITMO.matcher(trimmed).matches()) {
                return ResultadoValidacao.erro(
                        "Valor inválido para a métrica '" + titulo + "'. Verifique o formato esperado: ritmo no formato mm:ss.");
            }
            String[] partes = trimmed.split(":");
            int min = Integer.parseInt(partes[0]);
            int sec = Integer.parseInt(partes[1]);
            return ResultadoValidacao.ok(String.format("%02d:%02d", min, sec));
        }

        if (FORMATO_TEMPO_MMSS.matcher(trimmed).matches()) {
            String[] partes = trimmed.split(":");
            int min = Integer.parseInt(partes[0]);
            int sec = Integer.parseInt(partes[1]);
            return ResultadoValidacao.ok(String.format("%02d:%02d", min, sec));
        }

        if (FORMATO_TEMPO_HHMMSS.matcher(trimmed).matches()) {
            String[] partes = trimmed.split(":");
            int horas = Integer.parseInt(partes[0]);
            int min = Integer.parseInt(partes[1]);
            int sec = Integer.parseInt(partes[2]);
            return ResultadoValidacao.ok(String.format("%02d:%02d:%02d", horas, min, sec));
        }

        return ResultadoValidacao.erro(
                "Valor inválido para a métrica '" + titulo + "'. Verifique o formato esperado: deve estar no formato mm:ss ou hh:mm:ss.");
    }

    /**
     * Valida o valor informado conforme o tipo da métrica.
     * Retorna a mensagem de erro, ou null quando o valor é válido.
     */
    public static String validar(Metrica metrica, String valor) {
        return validarENormalizar(metrica, valor).mensagemErro();
    }
}
