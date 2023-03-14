package io.github.edsoncunha.upgrade.takehome.configuration;


import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfig {

//    @Bean
//    public OAuth2AuthorizedClientManager authorizedClientManager(
//            ClientRegistrationRepository clientRegistrationRepository,
//            OAuth2AuthorizedClientRepository authorizedClientRepository) {
//
//        OAuth2AuthorizedClientProvider authorizedClientProvider = //
//                OAuth2AuthorizedClientProviderBuilder.builder() //
//                        .authorizationCode() //
//                        .refreshToken() //
//                        .clientCredentials() //
//                        .password() //
//                        .build();
//
//        DefaultOAuth2AuthorizedClientManager authorizedClientManager =
//                new DefaultOAuth2AuthorizedClientManager( //
//                        clientRegistrationRepository, //
//                        authorizedClientRepository);
//        authorizedClientManager //
//                .setAuthorizedClientProvider(authorizedClientProvider);
//
//        return authorizedClientManager;
//    }
}