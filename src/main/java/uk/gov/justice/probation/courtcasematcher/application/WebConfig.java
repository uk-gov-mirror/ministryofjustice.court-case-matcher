package uk.gov.justice.probation.courtcasematcher.application;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfig {

    @Value("${court-case-service.base-url}")
    private String courtCaseServiceBaseUrl;

    @Value("${offender-search.base-url}")
    private String offenderSearchBaseUrl;

    @Value("${web.client.connect-timeout-ms}")
    private int connectTimeoutMs;

    @Value("${web.client.read-timeout-ms}")
    private int readTimeoutMs;

    @Value("${web.client.write-timeout-ms}")
    private int writeTimeoutMs;

    @Bean(name="court-case-service")
    public WebClient getCourtCaseServiceClient() {

        return defaultWebClientBuilder()
                .baseUrl(this.courtCaseServiceBaseUrl)
                .build();
    }

    @Bean(name="offender-search")
    public WebClient getOffenderSearchClient(OAuth2AuthorizedClientManager authorizedClientManager)
    {
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Client =
                new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
        return defaultWebClientBuilder()
                .baseUrl(this.offenderSearchBaseUrl)
                .filter(oauth2Client)
                .build();
    }

    private WebClient.Builder defaultWebClientBuilder() {
        HttpClient httpClient = HttpClient.create()
            .tcpConfiguration(client ->
                client.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                    .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(readTimeoutMs, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(writeTimeoutMs, TimeUnit.MILLISECONDS))));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }
}
