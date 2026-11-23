package com.gymnasion.tcc.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RelatorioPdfGeneratorTest {

    @Test
    @DisplayName("US-16: eixo Y do gráfico do PDF usa valores redondos que cobrem os dados")
    void ticksRedondos_cobremOsDados() {
        assertThat(RelatorioPdfGenerator.ticksRedondos(4.1, 7.35, 5)).containsExactly(4.0, 5.0, 6.0, 7.0, 8.0);
        assertThat(RelatorioPdfGenerator.ticksRedondos(256, 432, 5)).containsExactly(250.0, 300.0, 350.0, 400.0, 450.0);
        assertThat(RelatorioPdfGenerator.ticksRedondos(0, 10, 6)).containsExactly(0.0, 2.0, 4.0, 6.0, 8.0, 10.0);
        List<Double> iguais = RelatorioPdfGenerator.ticksRedondos(150, 150, 5);
        assertThat(iguais.get(0)).isLessThan(150);
        assertThat(iguais.get(iguais.size() - 1)).isGreaterThan(150);
    }

    @Test
    @DisplayName("US-16: datas do eixo X do PDF não ficam coladas e a última sempre aparece")
    void indicesRotulosX_semColisao() {
        float[] xs = {0, 20, 60, 61, 120, 180, 182};
        List<Integer> indices = RelatorioPdfGenerator.indicesRotulosX(xs, 10, 32);
        assertThat(indices).first().isEqualTo(0);
        assertThat(indices).last().isEqualTo(6);
        for (int i = 1; i < indices.size(); i++) {
            assertThat(xs[indices.get(i)] - xs[indices.get(i - 1)]).isGreaterThanOrEqualTo(32);
        }
    }
}
