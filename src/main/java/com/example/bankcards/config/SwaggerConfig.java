package com.example.bankcards.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    // Единое имя security схемы - должно совпадать с тем, что в контроллерах
    private static final String SECURITY_SCHEME_NAME = "BearerAuthentication";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bank Cards Management System API")
                        .version("2.0.0")
                        .description("""
                                ## 🏦 Система управления банковскими картами

                                ### 🔐 Авторизация:
                                1. Получите токен через `/api/v1/auth/authenticate`
                                2. Нажмите кнопку "Authorize"
                                3. Введите ваш токен
                                4. Теперь все защищенные запросы будут автоматически содержать заголовок Authorization
                                """))
                // Добавляем security requirement глобально
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("""
                                                **Введите JWT токен '**
                                                ❌ Неправильно: `Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`
                                                ✅ Правильно: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`
                                                Получите токен через эндпоинт `/api/v1/auth/authenticate`
                                                """)));
    }

    @Bean
    public GroupedOpenApi authenticationApi() {
        return GroupedOpenApi.builder()
                .group("authentication")
                .displayName("🔐 Аутентификация")
                .pathsToMatch("/api/v1/auth/**")
                .pathsToExclude("/api/v1/auth/register/admin")
                .build();
    }

    @Bean
    public GroupedOpenApi adminAuthenticationApi() {
        return GroupedOpenApi.builder()
                .group("admin-auth")
                .displayName("🔐 Аутентификация (Админ)")
                .pathsToMatch("/api/v1/auth/register/admin")
                .build();
    }

    @Bean
    public GroupedOpenApi adminCardsApi() {
        return GroupedOpenApi.builder()
                .group("admin-cards")
                .displayName("👑 Управление картами (Админ)")
                .pathsToMatch("/api/v1/admin/cards/**")
                .build();
    }

    //TODO No operations defined in spec!
    @Bean
    public GroupedOpenApi userCardsApi() {
        return GroupedOpenApi.builder()
                .group("user-cards")
                .displayName("💳 Мои карты")
                .pathsToMatch("/api/v1/user/cards/**")
                .build();
    }

    @Bean
    public GroupedOpenApi transactionsApi() {
        return GroupedOpenApi.builder()
                .group("transactions")
                .displayName("💰 Переводы")
                .pathsToMatch("/api/v1/transactions/**")
                .build();
    }

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("all")
                .displayName("📚 Все API")
                .pathsToMatch("/api/**")
                .build();
    }
}