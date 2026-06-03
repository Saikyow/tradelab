package com.fedor.tradelab.indicator;

import com.fedor.tradelab.service.IndicatorService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IndicatorServiceTest {

    private final IndicatorService service = new IndicatorService();

    @Test
    void sma_basicWindow() {
        List<Double> input = List.of(10.0, 12.0, 14.0, 16.0, 18.0);

        List<Double> result = service.sma(input, 3);

        // ожидаем: [null, null, 12.0, 14.0, 16.0]
        assertEquals(5, result.size());
        assertNull(result.get(0));
        assertNull(result.get(1));
        assertEquals(12.0, result.get(2), 0.0001);
        assertEquals(14.0, result.get(3), 0.0001);
        assertEquals(16.0, result.get(4), 0.0001);
    }

    @Test
    void sma_periodEqualsSize() {
        List<Double> input = List.of(2.0, 4.0, 6.0);

        List<Double> result = service.sma(input, 3);

        // только последний элемент посчитан: [null, null, 4.0]
        assertNull(result.get(0));
        assertNull(result.get(1));
        assertEquals(4.0, result.get(2), 0.0001);
    }

    @Test
    void sma_invalidPeriod_throws() {
        List<Double> input = List.of(1.0, 2.0, 3.0);

        // период <= 0 должен бросать исключение
        assertThrows(IllegalArgumentException.class,
                () -> service.sma(input, 0));
    }
}