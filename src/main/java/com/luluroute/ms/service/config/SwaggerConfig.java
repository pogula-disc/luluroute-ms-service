package com.luluroute.ms.service.config;


import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.paths.DefaultPathProvider;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.spring.web.plugins.WebFluxRequestHandlerProvider;
import springfox.documentation.spring.web.plugins.WebMvcRequestHandlerProvider;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebMvc
public class SwaggerConfig {

    public static final String SHIPMENT_SVC_TAG = "Shipment Service";

    @Autowired
    private AppConfig appConfig;

    @Value("${lulu-route.shipment-svc.swagger.host}")
    private String host;

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .host(host)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.luluroute.ms.service"))
                .paths(PathSelectors.any())
                .build().apiInfo(apiInfo())
                .securityContexts(Collections.singletonList(securityContext()))
                .securitySchemes(List.of(securitySchema()))
                .pathProvider(new DefaultPathProvider())
                .enable(true)
                .tags(new Tag(SHIPMENT_SVC_TAG, "This service used to process the create/cancel shipment requests"));
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder().title("Shipment Service API")
                .description("This Shipment Service API contains all the operations related to create and cancel shipments")
                .contact(new Contact("Luluroute 2.0", "", "luluRouteSupport@lululemon.com"))
                .version("1.0.0")
                .build();
    }

    private OAuth securitySchema() {
        List<AuthorizationScope> authorizationScopeList = new ArrayList<>();
        authorizationScopeList.add(new AuthorizationScope(appConfig.getLabelsScope(), "Label scope"));
        authorizationScopeList.add(new AuthorizationScope(appConfig.getRequestScope(), "Request scope"));
        GrantType clientCredentialsGrant = new ClientCredentialsGrant(appConfig.getAccessTokenUri());
        return new OAuth("oauth2", authorizationScopeList, Collections.singletonList(clientCredentialsGrant));
    }

    private SecurityContext securityContext() {
        return SecurityContext.builder().securityReferences(null).build();
    }

    @Bean
    public static BeanPostProcessor springfoxHandlerProviderBeanPostProcessor() {
        return new BeanPostProcessor() {

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof WebMvcRequestHandlerProvider || bean instanceof WebFluxRequestHandlerProvider) {
                    customizeSpringfoxHandlerMappings(getHandlerMappings(bean));
                }
                return bean;
            }

            private <T extends RequestMappingInfoHandlerMapping> void customizeSpringfoxHandlerMappings(List<T> mappings) {
                List<T> copy = mappings.stream()
                        .filter(mapping -> mapping.getPatternParser() == null)
                        .collect(Collectors.toList());
                mappings.clear();
                mappings.addAll(copy);
            }

            @SuppressWarnings("unchecked")
            private List<RequestMappingInfoHandlerMapping> getHandlerMappings(Object bean) {
                try {
                    Field field = ReflectionUtils.findField(bean.getClass(), "handlerMappings");
                    field.setAccessible(true);
                    return (List<RequestMappingInfoHandlerMapping>) field.get(bean);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }
}
