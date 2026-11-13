package com.gymnasion.tcc.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FormatadorTempoTest {

    @Test
    @DisplayName("formatarDuracaoMinutos(Integer) formata minutos, horas cheias e horas+minutos")
    void formatarDuracaoMinutos_integer() {
        assertThat(FormatadorTempo.formatarDuracaoMinutos((Integer) null)).isNull();
        assertThat(FormatadorTempo.formatarDuracaoMinutos(0)).isNull();
        assertThat(FormatadorTempo.formatarDuracaoMinutos(-10)).isNull();
        assertThat(FormatadorTempo.formatarDuracaoMinutos(45)).isEqualTo("45 min");
        assertThat(FormatadorTempo.formatarDuracaoMinutos(60)).isEqualTo("1h");
        assertThat(FormatadorTempo.formatarDuracaoMinutos(90)).isEqualTo("1h 30min");
        assertThat(FormatadorTempo.formatarDuracaoMinutos(125)).isEqualTo("2h 5min");
    }

    @Test
    @DisplayName("formatarDuracaoMinutos(Double) arredonda corretamente e ignora nao positivos")
    void formatarDuracaoMinutos_double() {
        assertThat(FormatadorTempo.formatarDuracaoMinutos((Double) null)).isNull();
        assertThat(FormatadorTempo.formatarDuracaoMinutos(0.0)).isNull();
        assertThat(FormatadorTempo.formatarDuracaoMinutos(0.4)).isNull();
        assertThat(FormatadorTempo.formatarDuracaoMinutos(45.4)).isEqualTo("45 min");
        assertThat(FormatadorTempo.formatarDuracaoMinutos(89.6)).isEqualTo("1h 30min");
    }

    @Test
    @DisplayName("formatarTempoSegundos e formatarDeltaTempoSegundos tratam mm:ss e hh:mm:ss")
    void formatarTempoEDelta() {
        assertThat(FormatadorTempo.formatarTempoSegundos(-5)).isEqualTo("00:00");
        assertThat(FormatadorTempo.formatarTempoSegundos(0)).isEqualTo("00:00");
        assertThat(FormatadorTempo.formatarTempoSegundos(65)).isEqualTo("01:05");
        assertThat(FormatadorTempo.formatarTempoSegundos(3599)).isEqualTo("59:59");
        assertThat(FormatadorTempo.formatarTempoSegundos(3600)).isEqualTo("01:00:00");
        assertThat(FormatadorTempo.formatarTempoSegundos(3665)).isEqualTo("01:01:05");

        assertThat(FormatadorTempo.formatarDeltaTempoSegundos(0)).isEqualTo("00:00");
        assertThat(FormatadorTempo.formatarDeltaTempoSegundos(75)).isEqualTo("+01:15");
        assertThat(FormatadorTempo.formatarDeltaTempoSegundos(-3661)).isEqualTo("-01:01:01");
    }

    @Test
    @DisplayName("parseTempoParaSegundos aceita mm:ss estendido (ritmo) e hh:mm:ss e rejeita invalidos")
    void parseTempoParaSegundos() {
        assertThat(FormatadorTempo.parseTempoParaSegundos(null)).isEmpty();
        assertThat(FormatadorTempo.parseTempoParaSegundos("   ")).isEmpty();
        assertThat(FormatadorTempo.parseTempoParaSegundos("00:00")).contains(0);
        assertThat(FormatadorTempo.parseTempoParaSegundos(" 5:03 ")).contains(303);
        assertThat(FormatadorTempo.parseTempoParaSegundos("120:00")).contains(7200);
        assertThat(FormatadorTempo.parseTempoParaSegundos("1:15:30")).contains(4530);
        assertThat(FormatadorTempo.parseTempoParaSegundos("10:00:00")).contains(36000);

        assertThat(FormatadorTempo.parseTempoParaSegundos("05:60")).isEmpty();
        assertThat(FormatadorTempo.parseTempoParaSegundos("01:60:00")).isEmpty();
        assertThat(FormatadorTempo.parseTempoParaSegundos("100:00:00")).isEmpty();
        assertThat(FormatadorTempo.parseTempoParaSegundos("abc")).isEmpty();
    }
}
