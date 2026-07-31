package com.roomconnect.shared.config;

import com.roomconnect.models.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebBindingConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new Converter<String, Category>() {
            @Override
            public Category convert(String source) {
                if (source == null || source.isBlank()) return null;
                try {
                    return Category.fromValue(source.trim());
                } catch (IllegalArgumentException e) {
                    try {
                        return Category.valueOf(source.trim().toUpperCase());
                    } catch (Exception ex) {
                        return null;
                    }
                }
            }
        });

        registry.addConverter(new Converter<String, AcType>() {
            @Override
            public AcType convert(String source) {
                if (source == null || source.isBlank()) return null;
                try {
                    return AcType.fromValue(source.trim());
                } catch (IllegalArgumentException e) {
                    try {
                        return AcType.valueOf(source.trim().toUpperCase());
                    } catch (Exception ex) {
                        return null;
                    }
                }
            }
        });

        registry.addConverter(new Converter<String, BathroomType>() {
            @Override
            public BathroomType convert(String source) {
                if (source == null || source.isBlank()) return null;
                for (BathroomType b : BathroomType.values()) {
                    if (b.name().equalsIgnoreCase(source.trim())) {
                        return b;
                    }
                }
                return null;
            }
        });

        registry.addConverter(new Converter<String, GenderPreference>() {
            @Override
            public GenderPreference convert(String source) {
                if (source == null || source.isBlank()) return null;
                for (GenderPreference g : GenderPreference.values()) {
                    if (g.name().equalsIgnoreCase(source.trim())) {
                        return g;
                    }
                }
                return null;
            }
        });
    }
}
