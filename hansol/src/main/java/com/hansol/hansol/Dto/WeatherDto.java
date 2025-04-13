package com.hansol.hansol.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WeatherDto {
    private String temperature;
    private String rain;
    private String weatherType;

    public WeatherDto(String temperature, String rain, String weatherType){
        this.temperature = temperature;
        this.rain = rain;
        this.weatherType = weatherType;
    }
}
