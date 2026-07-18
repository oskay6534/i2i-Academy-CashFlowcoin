package com.i2i.cryptopal.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.Set;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    private static final Set<String> PUBLIC_PATHS = Set.of(
        "/api/auth/register",
        "/api/auth/login"
    );

    @Bean
    public OpenAPI cryptoPalOpenApi() {
        return new OpenAPI()
            .info(
                new Info()
                    .title("CashFlowCoin API")
                    .description(
                        "Kripto para takip, portfoy, alim-satim ve yapay zeka API dokumani."
                    )
                    .version("1.0.0")
                    .contact(
                        new Contact()
                            .name("CashFlowCoin Team")
                    )
                    .license(
                        new License()
                            .name("Educational Project")
                    )
            )
            .components(
                new Components().addSecuritySchemes(
                    SECURITY_SCHEME,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("Session Token")
                )
            )
            .addSecurityItem(
                new SecurityRequirement().addList(SECURITY_SCHEME)
            );
    }

    @Bean
    public OpenApiCustomizer publicEndpointCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }

            PUBLIC_PATHS.forEach(path -> {
                if (openApi.getPaths().get(path) == null) {
                    return;
                }

                openApi.getPaths()
                    .get(path)
                    .readOperations()
                    .forEach(
                        operation -> operation.setSecurity(
                            Collections.emptyList()
                        )
                    );
            });
        };
    }
}