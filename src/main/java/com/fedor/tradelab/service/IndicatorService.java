package com.fedor.tradelab.service;


import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class IndicatorService {

    /**
     * Простая скользящая средняя (SMA).
     * @param values цены (обычно close)
     * @param period размер окна
     * @return список той же длины; первые (period-1) элементов = null
     */

    public List<Double> sma(List<Double> values, int period){
        if (period <= 0){
            throw new IllegalArgumentException("period must be greater than 0");
        }

        List<Double> result = new ArrayList<>();
        double windowSum = 0.0;

        for (int i=0; i < values.size(); i++){
            windowSum += values.get(i);

            if (i >= period){
                windowSum -= values.get(i - period);
            }

            if (i >= period - 1){
                result.add(windowSum / period);
            }else{
                result.add(null);
            }
        }
        return result;

    }
}
