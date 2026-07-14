package com.shopsphere.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Central ModelMapper configuration bean, shared by every mapper component in
 * the com.shopsphere.mapper package. STRICT matching avoids ModelMapper
 * silently guessing ambiguous property mappings, which is important once
 * entities have several fields of the same type (e.g. multiple BigDecimal
 * price fields on Product).
 */
@Configuration
public class MapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setFieldMatchingEnabled(true)
                .setSkipNullEnabled(true);
        return modelMapper;
    }
}
