package com.example.bankcards.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class SwaggerConfig {
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
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("""
                                                **Введите JWT токен**
                                                ❌ Неправильно: `Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`
                                                ✅ Правильно: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`
                                                Получите токен через эндпоинт `/api/v1/auth/authenticate`
                                                """)));
    }

    /**
     * Кастомайзер, который переопределяет схему Pageable,
     * чтобы в Swagger UI отображались нужные значения по умолчанию.
     */
    @Bean
    public OpenApiCustomizer pageableSchemaCustomizer() {
        return openApi -> {
            Schema<?> pageableSchema = new Schema<Map<String, Object>>()
                    .type("object")
                    .description("Параметры пагинации")
                    .addProperty("page", new IntegerSchema()
                            .description("Номер страницы (начиная с 0)")
                            .example(0)
                            ._default(0))
                    .addProperty("size", new IntegerSchema()
                            .description("Количество элементов на странице")
                            .example(20)
                            ._default(20))
                    .addProperty("sort", new ArraySchema()
                            .items(new StringSchema())
                            .description("Сортировка: поле,направление (например: balance,desc)")
                            .example(List.of("balance"))
                            ._default(List.of("balance")));

            // Устанавливаем пример для всего объекта
            pageableSchema.setExample(Map.of(
                    "page", 0,
                    "size", 20,
                    "sort", List.of("balance")
            ));

            // Добавляем/заменяем схему с именем "Pageable"
            openApi.getComponents().addSchemas("Pageable", pageableSchema);
        };
    }

    @Bean
    public GroupedOpenApi authenticationApi() {
        return GroupedOpenApi.builder()
                .group("authentication")
                .displayName("🔐 Аутентификация")
                .pathsToMatch("/api/v1/auth/**")
                .pathsToExclude("/api/v1/auth/register/admin")
                .addOpenApiCustomizer(pageableSchemaCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi adminAuthenticationApi() {
        return GroupedOpenApi.builder()
                .group("admin-auth")
                .displayName("🔐 Аутентификация (Админ)")
                .pathsToMatch("/api/v1/auth/register/admin")
                .addOpenApiCustomizer(pageableSchemaCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi adminCardsApi() {
        return GroupedOpenApi.builder()
                .group("admin-cards")
                .displayName("👑 Управление картами (Админ)")
                .pathsToMatch("/api/v1/admin/cards/**")
                .addOpenApiCustomizer(pageableSchemaCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi userCardsApi() {
        return GroupedOpenApi.builder()
                .group("user-cards")
                .displayName("💳 Мои карты")
                .pathsToMatch("/api/v1/user/cards/**")
                .addOpenApiCustomizer(pageableSchemaCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi transactionsApi() {
        return GroupedOpenApi.builder()
                .group("transactions")
                .displayName("💰 Переводы")
                .pathsToMatch("/api/v1/transactions/**")
                .addOpenApiCustomizer(pageableSchemaCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("all")
                .displayName("📚 Все API")
                .pathsToMatch("/api/**")
                .addOpenApiCustomizer(pageableSchemaCustomizer())
                .build();
    }
}