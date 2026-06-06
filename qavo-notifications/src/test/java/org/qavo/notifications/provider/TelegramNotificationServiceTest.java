/* SPDX-License-Identifier: MIT — Copyright 2026 Qavo. See LICENSE. */
package org.qavo.notifications.provider;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qavo.core.notifications.NotificationChannel;
import org.qavo.core.notifications.NotificationRequest;
import org.qavo.core.notifications.NotificationResult;
import org.qavo.notifications.config.QavoNotificationsProperties;
import org.qavo.resilience.autoconfigure.QavoHttpClientAutoConfiguration;
import org.qavo.resilience.http.QavoHttpClientRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * End-to-end test of {@link TelegramNotificationService}: drives the provider against a
 * WireMock-stubbed Telegram Bot API and asserts both the request shape (POST body with
 * {@code chat_id} and {@code text}) and the response handling (success/failure result
 * mapping). Validates the supports contract.
 */
class TelegramNotificationServiceTest {

    private WireMockServer wireMock;

    @BeforeEach
    void startWireMock() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
    }

    @AfterEach
    void stopWireMock() {
        if (wireMock != null) {
            wireMock.stop();
        }
    }

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration.class,
                        io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration.class,
                        QavoHttpClientAutoConfiguration.class))
                .withUserConfiguration(TestTraceContextConfig.class)
                .withPropertyValues(
                        "qavo.resilience.http.clients.telegram.base-url=" + wireMock.baseUrl());
    }

    private QavoNotificationsProperties.Telegram props() {
        QavoNotificationsProperties.Telegram tg = new QavoNotificationsProperties.Telegram();
        tg.setEnabled(true);
        tg.setBotToken("TEST_TOKEN");
        tg.setClientName("telegram");
        return tg;
    }

    @Test
    void delivers_message_via_bot_api() {
        wireMock.stubFor(post(urlEqualTo("/botTEST_TOKEN/sendMessage"))
                .withRequestBody(equalToJson("{\"chat_id\":\"42\",\"text\":\"hi\"}", true, true))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ok\":true,\"result\":{\"message_id\":987}}")));

        runner().run(context -> {
            TelegramNotificationService service = new TelegramNotificationService(
                    context.getBean(QavoHttpClientRegistry.class), props());

            NotificationResult result = service.send(NotificationRequest.telegram("42", "hi"));

            assertThat(result.success()).isTrue();
            assertThat(result.providerMessageId()).isEqualTo("987");
        });

        wireMock.verify(1, postRequestedFor(urlEqualTo("/botTEST_TOKEN/sendMessage")));
    }

    @Test
    void returns_failure_when_api_responds_non_2xx() {
        wireMock.stubFor(post(urlEqualTo("/botTEST_TOKEN/sendMessage"))
                .willReturn(aResponse().withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ok\":false,\"description\":\"Unauthorized\"}")));

        runner().run(context -> {
            TelegramNotificationService service = new TelegramNotificationService(
                    context.getBean(QavoHttpClientRegistry.class), props());

            NotificationResult result = service.send(NotificationRequest.telegram("42", "hi"));

            assertThat(result.success()).isFalse();
            // The 401 surfaces as an exception from the Spring RestClient and is caught soft.
            assertThat(result.errorMessage()).isNotBlank();
        });
    }

    @Test
    void fails_softly_when_bot_token_blank() {
        QavoNotificationsProperties.Telegram blank = new QavoNotificationsProperties.Telegram();
        blank.setEnabled(true);
        blank.setClientName("telegram");

        runner().run(context -> {
            TelegramNotificationService service = new TelegramNotificationService(
                    context.getBean(QavoHttpClientRegistry.class), blank);

            NotificationResult result = service.send(NotificationRequest.telegram("42", "hi"));

            assertThat(result.success()).isFalse();
            assertThat(result.errorMessage()).contains("qavo.notifications.telegram.bot-token");
        });
    }

    @Test
    void supports_only_telegram_channel() {
        runner().run(context -> {
            TelegramNotificationService service = new TelegramNotificationService(
                    context.getBean(QavoHttpClientRegistry.class), props());

            assertThat(service.supports(NotificationChannel.TELEGRAM)).isTrue();
            assertThat(service.supports(NotificationChannel.EMAIL)).isFalse();
            assertThat(service.supports(NotificationChannel.NONE)).isFalse();
        });
    }
}
