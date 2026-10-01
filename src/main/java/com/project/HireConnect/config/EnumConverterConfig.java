package com.project.HireConnect.config;

import com.project.HireConnect.enums.VacanteExperienciaRequerida;
import com.project.HireConnect.enums.VacanteModalidad;
import com.project.HireConnect.enums.VacanteTipocontrato;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class EnumConverterConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(@NonNull FormatterRegistry registry) {
        registry.addConverter(new StringToVacanteModalidadConverter());
        registry.addConverter(new StringToVacanteTipocontratoConverter());
        registry.addConverter(new StringToVacanteExperienciaRequeridaConverter());
    }

    private static class StringToVacanteModalidadConverter implements Converter<String, VacanteModalidad> {
        @Override
        public VacanteModalidad convert(@NonNull String source) {
            try {
                return VacanteModalidad.fromDescripcion(source);
            } catch (IllegalArgumentException e) {
                return VacanteModalidad.valueOf(source);
            }
        }
    }

    private static class StringToVacanteTipocontratoConverter implements Converter<String, VacanteTipocontrato> {
        @Override
        public VacanteTipocontrato convert(@NonNull String source) {
            try {
                return VacanteTipocontrato.fromDescripcion(source);
            } catch (IllegalArgumentException e) {
                return VacanteTipocontrato.valueOf(source);
            }
        }
    }

    private static class StringToVacanteExperienciaRequeridaConverter implements Converter<String, VacanteExperienciaRequerida> {
        @Override
        public VacanteExperienciaRequerida convert(@NonNull String source) {
            try {
                return VacanteExperienciaRequerida.fromDescripcion(source);
            } catch (IllegalArgumentException e) {
                return VacanteExperienciaRequerida.valueOf(source);
            }
        }
    }
}