package com.gymnasion.tcc.domain.enums;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

/**
 * Mapeamento de campos retornados pela API v3 do Strava para métricas personalizadas (US-14 / US-18).
 */
public enum CampoStrava {
    DISTANCIA_KM,
    DISTANCIA_METROS,
    TEMPO_MOVIMENTO,
    TEMPO_TOTAL,
    DURACAO_MINUTOS,
    VELOCIDADE_MEDIA_KMH,
    VELOCIDADE_MAXIMA_KMH,
    RITMO_MEDIO,
    FREQUENCIA_CARDIACA_MEDIA,
    FREQUENCIA_CARDIACA_MAXIMA,
    CADENCIA_MEDIA,
    POTENCIA_MEDIA_WATTS,
    POTENCIA_MAXIMA_WATTS,
    ELEVACAO_GANHO_METROS,
    CALORIAS,
    NENHUM;

    public static final Set<String> UNIDADES_NUMERICO = Set.of(
            "kg", "km", "m", "cm", "%", "reps", "rounds", "séries",
            "kcal", "min", "km/h", "W", "spm", "rpm", "pontos"
    );

    public static final Set<String> UNIDADES_TEMPO_CRONOMETRO = Set.of("mm:ss", "hh:mm:ss");
    public static final Set<String> UNIDADES_TEMPO_RITMO = Set.of("min/km", "min/100m", "min/500m");
    public static final Set<String> UNIDADES_TEMPO = Set.of("mm:ss", "hh:mm:ss", "min/km", "min/100m", "min/500m");
    public static final String UNIDADE_BPM = "bpm";

    public record TipoEUnidade(TipoMetrica tipo, String unidade) {}

    public boolean isCompativelCom(TipoMetrica tipo) {
        if (this == NENHUM || tipo == null) {
            return true;
        }
        return switch (this) {
            case DISTANCIA_KM, DISTANCIA_METROS, DURACAO_MINUTOS,
                 VELOCIDADE_MEDIA_KMH, VELOCIDADE_MAXIMA_KMH,
                 CADENCIA_MEDIA, POTENCIA_MEDIA_WATTS, POTENCIA_MAXIMA_WATTS,
                 ELEVACAO_GANHO_METROS, CALORIAS -> tipo == TipoMetrica.NUMERICO;
            case TEMPO_MOVIMENTO, TEMPO_TOTAL, RITMO_MEDIO -> tipo == TipoMetrica.TEMPO;
            case FREQUENCIA_CARDIACA_MEDIA, FREQUENCIA_CARDIACA_MAXIMA -> tipo == TipoMetrica.FREQUENCIA_CARDIACA;
            case NENHUM -> true;
        };
    }

    public boolean isCompativelCom(TipoMetrica tipo, String unidade) {
        if (!isCompativelCom(tipo)) {
            return false;
        }
        if (this == NENHUM || tipo == null) {
            return true;
        }
        if (unidade == null) {
            return this != RITMO_MEDIO;
        }
        return switch (this) {
            case DISTANCIA_KM -> "km".equals(unidade);
            case DISTANCIA_METROS, ELEVACAO_GANHO_METROS -> "m".equals(unidade);
            case DURACAO_MINUTOS -> "min".equals(unidade);
            case VELOCIDADE_MEDIA_KMH, VELOCIDADE_MAXIMA_KMH -> "km/h".equals(unidade);
            case CADENCIA_MEDIA -> "rpm".equals(unidade) || "spm".equals(unidade);
            case POTENCIA_MEDIA_WATTS, POTENCIA_MAXIMA_WATTS -> "W".equals(unidade);
            case CALORIAS -> "kcal".equals(unidade);
            case TEMPO_MOVIMENTO, TEMPO_TOTAL -> UNIDADES_TEMPO_CRONOMETRO.contains(unidade);
            case RITMO_MEDIO -> UNIDADES_TEMPO_RITMO.contains(unidade);
            case FREQUENCIA_CARDIACA_MEDIA, FREQUENCIA_CARDIACA_MAXIMA -> UNIDADE_BPM.equals(unidade);
            case NENHUM -> true;
        };
    }

    public static boolean isUnidadeValida(TipoMetrica tipo, String unidade) {
        if (tipo == null) {
            return false;
        }
        return switch (tipo) {
            case NUMERICO -> unidade == null || UNIDADES_NUMERICO.contains(unidade);
            case TEMPO -> unidade != null && UNIDADES_TEMPO.contains(unidade);
            case FREQUENCIA_CARDIACA -> UNIDADE_BPM.equals(unidade);
            case TEXTO -> unidade == null;
        };
    }

    public static boolean isUnidadeRitmo(String unidade) {
        return unidade != null && UNIDADES_TEMPO_RITMO.contains(unidade);
    }

    public static TipoEUnidade sugerirTipoEUnidade(CampoStrava campo, String nomeModalidade) {
        if (campo == null || campo == NENHUM) {
            return null;
        }
        String mod = normalizar(nomeModalidade);
        boolean isNatacao = mod.contains("natac") || mod.contains("swim");
        boolean isRemo = mod.contains("remo") || mod.contains("rowing");
        boolean isCiclismo = mod.contains("ciclismo") || mod.contains("spinning");
        boolean isEndurance = isCiclismo || mod.contains("triatl") || mod.contains("triathl")
                || mod.contains("duatl") || mod.contains("ironman");

        return switch (campo) {
            case DISTANCIA_KM -> new TipoEUnidade(TipoMetrica.NUMERICO, "km");
            case DISTANCIA_METROS, ELEVACAO_GANHO_METROS -> new TipoEUnidade(TipoMetrica.NUMERICO, "m");
            case DURACAO_MINUTOS -> new TipoEUnidade(TipoMetrica.NUMERICO, "min");
            case VELOCIDADE_MEDIA_KMH, VELOCIDADE_MAXIMA_KMH -> new TipoEUnidade(TipoMetrica.NUMERICO, "km/h");
            case POTENCIA_MEDIA_WATTS, POTENCIA_MAXIMA_WATTS -> new TipoEUnidade(TipoMetrica.NUMERICO, "W");
            case CALORIAS -> new TipoEUnidade(TipoMetrica.NUMERICO, "kcal");
            case CADENCIA_MEDIA -> new TipoEUnidade(TipoMetrica.NUMERICO, isCiclismo ? "rpm" : "spm");
            case FREQUENCIA_CARDIACA_MEDIA, FREQUENCIA_CARDIACA_MAXIMA -> new TipoEUnidade(TipoMetrica.FREQUENCIA_CARDIACA, UNIDADE_BPM);
            case TEMPO_MOVIMENTO, TEMPO_TOTAL -> new TipoEUnidade(TipoMetrica.TEMPO, isEndurance ? "hh:mm:ss" : "mm:ss");
            case RITMO_MEDIO -> new TipoEUnidade(
                    TipoMetrica.TEMPO,
                    isNatacao ? "min/100m" : (isRemo ? "min/500m" : "min/km")
            );
            case NENHUM -> null;
        };
    }

    private static String normalizar(String s) {
        if (s == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT).trim();
    }
}
