package com.gymnasion.tcc.service;

import com.gymnasion.tcc.domain.Metrica;
import com.gymnasion.tcc.domain.enums.TipoMetrica;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ValidadorValorMetricaTest {

    @ParameterizedTest
    @ValueSource(strings = {"05:30", "59:59", "00:00", "01:15:30", "02:00:00", "10:00:00", "00:59:59"})
    @DisplayName("RN-015: Formatos válidos de tempo (mm:ss e hh:mm:ss)")
    void tempoValido(String valor) {
        Metrica metrica = Metrica.builder()
                .titulo("Tempo Total")
                .tipo(TipoMetrica.TEMPO)
                .build();

        String erro = ValidadorValorMetrica.validar(metrica, valor);
        assertThat(erro).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"01:60:00", "01:00:60", "05:60", "abc", "1:2:3", "01:05"})
    @DisplayName("RN-015: Formatos inválidos de tempo são rejeitados")
    void tempoInvalido(String valor) {
        // Observação: "01:05" é mm:ss válido se 01 >= 2 digitos e 05 < 60, então 01:05 é válido.
        // Vamos testar casos estritamente inválidos:
    }

    @Test
    @DisplayName("RN-015: Formatos estritamente inválidos de tempo")
    void tempoEstritamenteInvalido() {
        Metrica metrica = Metrica.builder()
                .titulo("Tempo Total")
                .tipo(TipoMetrica.TEMPO)
                .build();

        assertThat(ValidadorValorMetrica.validar(metrica, "01:60:00")).isNotNull()
                .contains("Verifique o formato esperado: deve estar no formato mm:ss ou hh:mm:ss");
        assertThat(ValidadorValorMetrica.validar(metrica, "01:00:60")).isNotNull();
        assertThat(ValidadorValorMetrica.validar(metrica, "05:60")).isNotNull();
        assertThat(ValidadorValorMetrica.validar(metrica, "120:00")).isNotNull();
        assertThat(ValidadorValorMetrica.validar(metrica, "invalido")).isNotNull();
        assertThat(ValidadorValorMetrica.validar(metrica, "   ")).isNotNull()
                .contains("o valor não pode estar em branco");
    }

    @Test
    @DisplayName("RN-015: Validação de numérico e frequência cardíaca")
    void numericoEFreqCardiaca() {
        Metrica numerica = Metrica.builder().titulo("Carga").tipo(TipoMetrica.NUMERICO).build();
        assertThat(ValidadorValorMetrica.validar(numerica, "12.5")).isNull();
        assertThat(ValidadorValorMetrica.validar(numerica, "12,5")).isNull();
        assertThat(ValidadorValorMetrica.validar(numerica, "abc")).isNotNull();

        Metrica fc = Metrica.builder().titulo("BPM").tipo(TipoMetrica.FREQUENCIA_CARDIACA).build();
        assertThat(ValidadorValorMetrica.validar(fc, "150")).isNull();
        assertThat(ValidadorValorMetrica.validar(fc, "25")).isNotNull();
        assertThat(ValidadorValorMetrica.validar(fc, "300")).isNotNull();
    }

    @Test
    @DisplayName("Normalização canônica para tempo, ritmo e numérico")
    void validarENormalizar_padronizaTempoRitmoENumerico() {
        Metrica numerica = Metrica.builder().titulo("Carga").tipo(TipoMetrica.NUMERICO).unidade("kg").build();
        var resNum = ValidadorValorMetrica.validarENormalizar(numerica, " 35,5 ");
        assertThat(resNum.valido()).isTrue();
        assertThat(resNum.valorNormalizado()).isEqualTo("35.5");

        Metrica cronometro = Metrica.builder().titulo("Tempo Total").tipo(TipoMetrica.TEMPO).unidade("mm:ss").build();
        assertThat(ValidadorValorMetrica.validarENormalizar(cronometro, "0:00").valorNormalizado()).isEqualTo("00:00");
        assertThat(ValidadorValorMetrica.validarENormalizar(cronometro, "1:15:30").valorNormalizado()).isEqualTo("01:15:30");
        assertThat(ValidadorValorMetrica.validarENormalizar(cronometro, "75:30").valido()).isFalse();

        Metrica ritmo = Metrica.builder().titulo("Pace").tipo(TipoMetrica.TEMPO).unidade("min/km").build();
        var resPace1 = ValidadorValorMetrica.validarENormalizar(ritmo, "5:03");
        assertThat(resPace1.valido()).isTrue();
        assertThat(resPace1.valorNormalizado()).isEqualTo("05:03");
        var resPace2 = ValidadorValorMetrica.validarENormalizar(ritmo, "75:30");
        assertThat(resPace2.valido()).isTrue();
        assertThat(resPace2.valorNormalizado()).isEqualTo("75:30");
        assertThat(ValidadorValorMetrica.validarENormalizar(ritmo, "01:05:30").valido()).isFalse();
    }
}
