package com.gymnasion.tcc.service;

import java.util.Optional;

/**
 * Utilitário centralizado para formatação e parsing de durações e métricas temporais.
 */
public final class FormatadorTempo {

    private FormatadorTempo() {}

    public static String formatarDuracaoMinutos(Integer minutos) {
        if (minutos == null || minutos <= 0) {
            return null;
        }
        if (minutos < 60) {
            return minutos + " min";
        }
        int horas = minutos / 60;
        int resto = minutos % 60;
        if (resto == 0) {
            return horas + "h";
        }
        return horas + "h " + resto + "min";
    }

    public static String formatarDuracaoMinutos(Double minutos) {
        if (minutos == null || minutos <= 0) {
            return null;
        }
        int arredondado = (int) Math.round(minutos);
        if (arredondado <= 0) {
            return null;
        }
        return formatarDuracaoMinutos(arredondado);
    }

    public static String formatarTempoSegundos(int totalSegundos) {
        int seguro = Math.max(0, totalSegundos);
        if (seguro < 3600) {
            int mins = seguro / 60;
            int segs = seguro % 60;
            return String.format("%02d:%02d", mins, segs);
        }
        int horas = seguro / 3600;
        int mins = (seguro % 3600) / 60;
        int segs = seguro % 60;
        return String.format("%02d:%02d:%02d", horas, mins, segs);
    }

    public static String formatarDeltaTempoSegundos(int deltaSegundos) {
        String sinal = deltaSegundos > 0 ? "+" : (deltaSegundos < 0 ? "-" : "");
        return sinal + formatarTempoSegundos(Math.abs(deltaSegundos));
    }

    public static Optional<Integer> parseTempoParaSegundos(String valor) {
        if (valor == null) {
            return Optional.empty();
        }
        String trimmed = valor.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        String[] partes = trimmed.split(":", -1);
        try {
            if (partes.length == 2) {
                if (!partes[0].matches("\\d+") || !partes[1].matches("[0-5]\\d")) {
                    return Optional.empty();
                }
                int mins = Integer.parseInt(partes[0]);
                int segs = Integer.parseInt(partes[1]);
                return Optional.of(mins * 60 + segs);
            }
            if (partes.length == 3) {
                if (!partes[0].matches("\\d{1,2}") || !partes[1].matches("[0-5]\\d") || !partes[2].matches("[0-5]\\d")) {
                    return Optional.empty();
                }
                int horas = Integer.parseInt(partes[0]);
                int mins = Integer.parseInt(partes[1]);
                int segs = Integer.parseInt(partes[2]);
                return Optional.of(horas * 3600 + mins * 60 + segs);
            }
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }
}
