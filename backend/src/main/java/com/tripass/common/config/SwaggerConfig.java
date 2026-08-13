package com.tripass.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.service.SecurityScheme;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.List;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.tripass"))
                .paths(PathSelectors.ant("/api/v1/**"))
                .build()
                .apiInfo(apiInfo())
                .securitySchemes(List.of(jwtSecurityScheme()))
                .securityContexts(List.of(
                        jwtSecurityContext(),
                        authPasswordChangeSecurityContext()
                ));
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("TRIPass API")
                .description("TRIPass 백엔드 API 문서\n"
                        + "Authorize 입력 시 `Bearer {Access Token}` 전체 값을 입력하세요.")
                .version("1.0")
                .build();
    }
    private SecurityScheme jwtSecurityScheme() {
        return new ApiKey(
                "JWT",
                "Authorization",
                "header"
        );
    }
    private SecurityContext jwtSecurityContext() {
        return SecurityContext
                .builder()
                .securityReferences(
                        jwtSecurityReferences()
                )
                .forPaths(
                        PathSelectors.regex(
                                "/api/v1/(?!auth(?:/|$)).*"
                        )
                )
                .build();
    }

    private SecurityContext authPasswordChangeSecurityContext() {
        return SecurityContext
                .builder()
                .securityReferences(
                        jwtSecurityReferences()
                )
                .forPaths(
                        PathSelectors.ant(
                                "/api/v1/auth/password/change"
                        )
                )
                .build();
    }
    private List<SecurityReference> jwtSecurityReferences() {

        AuthorizationScope authorizationScope =
                new AuthorizationScope(
                        "global",
                        "Access Token을 이용한 API 접근"
                );

        return List.of(
                new SecurityReference(
                        "JWT",
                        new AuthorizationScope[]{
                                authorizationScope
                        }
                )
        );
    }
}
