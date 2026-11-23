package com.gymnasion.tcc.domain.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CampoStravaTest {

    @Test
    @DisplayName("Compatibilidade de CampoStrava com TipoMetrica e unidade")
    void isCompativelCom_tipoEUnidade() {
        // RITMO_MEDIO agora pertence apenas a TEMPO com unidade de ritmo
        assertThat(CampoStrava.RITMO_MEDIO.isCompativelCom(TipoMetrica.NUMERICO)).isFalse();
        assertThat(CampoStrava.RITMO_MEDIO.isCompativelCom(TipoMetrica.TEMPO)).isTrue();
        assertThat(CampoStrava.RITMO_MEDIO.isCompativelCom(TipoMetrica.TEMPO, "min/km")).isTrue();
        assertThat(CampoStrava.RITMO_MEDIO.isCompativelCom(TipoMetrica.TEMPO, "min/100m")).isTrue();
        assertThat(CampoStrava.RITMO_MEDIO.isCompativelCom(TipoMetrica.TEMPO, "min/500m")).isTrue();
        assertThat(CampoStrava.RITMO_MEDIO.isCompativelCom(TipoMetrica.TEMPO, "mm:ss")).isFalse();
        assertThat(CampoStrava.RITMO_MEDIO.isCompativelCom(TipoMetrica.TEMPO, null)).isFalse();

        // TEMPO_MOVIMENTO / TEMPO_TOTAL exigem TEMPO com unidade de cronômetro (ou null/blank)
        assertThat(CampoStrava.TEMPO_MOVIMENTO.isCompativelCom(TipoMetrica.TEMPO, "mm:ss")).isTrue();
        assertThat(CampoStrava.TEMPO_TOTAL.isCompativelCom(TipoMetrica.TEMPO, "hh:mm:ss")).isTrue();
        assertThat(CampoStrava.TEMPO_MOVIMENTO.isCompativelCom(TipoMetrica.TEMPO, null)).isTrue();
        assertThat(CampoStrava.TEMPO_MOVIMENTO.isCompativelCom(TipoMetrica.TEMPO, "min/km")).isFalse();

        // FC exige FREQUENCIA_CARDIACA e bpm (ou null)
        assertThat(CampoStrava.FREQUENCIA_CARDIACA_MEDIA.isCompativelCom(TipoMetrica.FREQUENCIA_CARDIACA, "bpm")).isTrue();
        assertThat(CampoStrava.FREQUENCIA_CARDIACA_MAXIMA.isCompativelCom(TipoMetrica.FREQUENCIA_CARDIACA, null)).isTrue();
        assertThat(CampoStrava.FREQUENCIA_CARDIACA_MEDIA.isCompativelCom(TipoMetrica.FREQUENCIA_CARDIACA, "W")).isFalse();
        assertThat(CampoStrava.FREQUENCIA_CARDIACA_MEDIA.isCompativelCom(TipoMetrica.NUMERICO, "bpm")).isFalse();

        // Numéricos aceitam null ou unidade correspondente ao campo
        assertThat(CampoStrava.DISTANCIA_KM.isCompativelCom(TipoMetrica.NUMERICO, "km")).isTrue();
        assertThat(CampoStrava.DISTANCIA_KM.isCompativelCom(TipoMetrica.NUMERICO, null)).isTrue();
        assertThat(CampoStrava.DISTANCIA_KM.isCompativelCom(TipoMetrica.NUMERICO, "kcal")).isFalse();
        assertThat(CampoStrava.CALORIAS.isCompativelCom(TipoMetrica.NUMERICO, "kcal")).isTrue();

        // NENHUM aceita qualquer combinação válida
        assertThat(CampoStrava.NENHUM.isCompativelCom(TipoMetrica.TEXTO, null)).isTrue();
        assertThat(CampoStrava.NENHUM.isCompativelCom(TipoMetrica.NUMERICO, "kg")).isTrue();
    }

    @Test
    @DisplayName("isUnidadeValida e isUnidadeRitmo validam unidades por tipo de métrica")
    void isUnidadeValida_e_isUnidadeRitmo() {
        assertThat(CampoStrava.isUnidadeValida(TipoMetrica.TEXTO, null)).isTrue();
        assertThat(CampoStrava.isUnidadeValida(TipoMetrica.TEXTO, "   ")).isFalse();
        assertThat(CampoStrava.isUnidadeValida(TipoMetrica.TEXTO, "kg")).isFalse();

        assertThat(CampoStrava.isUnidadeValida(TipoMetrica.FREQUENCIA_CARDIACA, "bpm")).isTrue();
        assertThat(CampoStrava.isUnidadeValida(TipoMetrica.FREQUENCIA_CARDIACA, "W")).isFalse();

        assertThat(CampoStrava.isUnidadeValida(TipoMetrica.TEMPO, "mm:ss")).isTrue();
        assertThat(CampoStrava.isUnidadeValida(TipoMetrica.TEMPO, "min/100m")).isTrue();
        assertThat(CampoStrava.isUnidadeValida(TipoMetrica.TEMPO, "km")).isFalse();

        assertThat(CampoStrava.isUnidadeValida(TipoMetrica.NUMERICO, "W")).isTrue();
        assertThat(CampoStrava.isUnidadeValida(TipoMetrica.NUMERICO, null)).isTrue();
        assertThat(CampoStrava.isUnidadeValida(TipoMetrica.NUMERICO, "invalida")).isFalse();

        assertThat(CampoStrava.isUnidadeRitmo("min/km")).isTrue();
        assertThat(CampoStrava.isUnidadeRitmo("min/100m")).isTrue();
        assertThat(CampoStrava.isUnidadeRitmo("min/500m")).isTrue();
        assertThat(CampoStrava.isUnidadeRitmo("mm:ss")).isFalse();
        assertThat(CampoStrava.isUnidadeRitmo(null)).isFalse();
    }

    @Test
    @DisplayName("sugerirTipoEUnidade sugere tipo e unidade por campo e modalidade")
    void sugerirTipoEUnidade() {
        assertThat(CampoStrava.sugerirTipoEUnidade(CampoStrava.RITMO_MEDIO, "Natação"))
                .isEqualTo(new CampoStrava.TipoEUnidade(TipoMetrica.TEMPO, "min/100m"));
        assertThat(CampoStrava.sugerirTipoEUnidade(CampoStrava.RITMO_MEDIO, "Remo"))
                .isEqualTo(new CampoStrava.TipoEUnidade(TipoMetrica.TEMPO, "min/500m"));
        assertThat(CampoStrava.sugerirTipoEUnidade(CampoStrava.RITMO_MEDIO, "Corrida"))
                .isEqualTo(new CampoStrava.TipoEUnidade(TipoMetrica.TEMPO, "min/km"));
        assertThat(CampoStrava.sugerirTipoEUnidade(CampoStrava.FREQUENCIA_CARDIACA_MEDIA, "Corrida"))
                .isEqualTo(new CampoStrava.TipoEUnidade(TipoMetrica.FREQUENCIA_CARDIACA, "bpm"));
        assertThat(CampoStrava.sugerirTipoEUnidade(CampoStrava.ELEVACAO_GANHO_METROS, "Ciclismo"))
                .isEqualTo(new CampoStrava.TipoEUnidade(TipoMetrica.NUMERICO, "m"));
        assertThat(CampoStrava.sugerirTipoEUnidade(CampoStrava.NENHUM, "Corrida")).isNull();
        assertThat(CampoStrava.sugerirTipoEUnidade(null, "Corrida")).isNull();
    }
}
