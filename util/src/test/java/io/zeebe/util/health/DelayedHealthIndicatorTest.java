/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.util.health;

import java.time.Duration;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

@RunWith(MockitoJUnitRunner.class)
public class DelayedHealthIndicatorTest {

  private static final Duration TEST_MAX_DOWNTIME = Duration.ofMillis(50);

  @Mock private HealthIndicator mockHealthIndicator;

  @Test
  public void shouldRejectNullHealthIndicatorInConstructor() {
    Assertions.assertThatThrownBy(() -> new DelayedHealthIndicator(null, TEST_MAX_DOWNTIME))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void shouldRejectNullMaxDowntimeInConstructor() {
    Assertions.assertThatThrownBy(() -> new DelayedHealthIndicator(mockHealthIndicator, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void shouldRejectNegativeMaxDowntimeInConstructor() {
    Assertions.assertThatThrownBy(
            () -> new DelayedHealthIndicator(mockHealthIndicator, Duration.ofMillis(-50)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void shouldReportUnknownHealthStatusIfAskedBeforeDelegateHealthIndicatorWasCalled() {
    // given
    final var sutDelayedHealthIndicator =
        new DelayedHealthIndicator(mockHealthIndicator, TEST_MAX_DOWNTIME);

    // when
    final Health actualHealth = sutDelayedHealthIndicator.health();

    // then
    Assertions.assertThat(actualHealth).isNotNull();
    Assertions.assertThat(actualHealth.getStatus()).isEqualTo(Status.UNKNOWN);
  }

  @Test
  public void
      shouldReportHealthStatusOfDelegateHealthIndicatorIfBackendHealthIndicatorWasNeverUp() {
    // given
    final var sutDelayedHealthIndicator =
        new DelayedHealthIndicator(mockHealthIndicator, TEST_MAX_DOWNTIME);

    Mockito.when(mockHealthIndicator.health()).thenReturn(Health.down().build());

    // when
    sutDelayedHealthIndicator.checkHealth();
    final Health actualHealth = sutDelayedHealthIndicator.health();

    // then
    Assertions.assertThat(actualHealth).isNotNull();
    Assertions.assertThat(actualHealth.getStatus()).isEqualTo(Status.DOWN);
  }

  @Test
  public void
      shouldReportHealthStatusUpWhenBackendHealthIndicatorWasUpInThePastAndIsTemporarilyDown() {
    // given
    final var sutDelayedHealthIndicator =
        new DelayedHealthIndicator(mockHealthIndicator, TEST_MAX_DOWNTIME);
    // backend health indicator was up in the past
    Mockito.when(mockHealthIndicator.health()).thenReturn(Health.up().build());
    sutDelayedHealthIndicator.checkHealth();

    // when
    // backend health indicator goes down
    Mockito.when(mockHealthIndicator.health()).thenReturn(Health.down().build());
    sutDelayedHealthIndicator.checkHealth();

    final Health actualHealth = sutDelayedHealthIndicator.health();

    // then
    // delayed health indicator is still up
    Assertions.assertThat(actualHealth).isNotNull();
    Assertions.assertThat(actualHealth.getStatus()).isEqualTo(Status.UP);
  }

  @Test
  public void
      shouldReportHealthStatusDownWhenBackendHealthIndicatorWasUpInThePastAndIsDownForMoreThanMaxDowntime()
          throws InterruptedException {
    // given
    final var sutDelayedHealthIndicator =
        new DelayedHealthIndicator(mockHealthIndicator, TEST_MAX_DOWNTIME);
    // backend health indicator was up in the past
    Mockito.when(mockHealthIndicator.health()).thenReturn(Health.up().build());
    sutDelayedHealthIndicator.checkHealth();

    // when
    // backend health indicator goes down
    Mockito.when(mockHealthIndicator.health()).thenReturn(Health.down().build());
    sutDelayedHealthIndicator.checkHealth();

    final Health actualHealthImmediate = sutDelayedHealthIndicator.health();

    // wait for more then the configured max downtime
    Thread.sleep(100);
    sutDelayedHealthIndicator.checkHealth();
    final Health actualHealthAfterDelay = sutDelayedHealthIndicator.health();

    // then
    // immediate health report was up
    Assertions.assertThat(actualHealthImmediate).isNotNull();
    Assertions.assertThat(actualHealthImmediate.getStatus()).isEqualTo(Status.UP);

    // delayed health report was down
    Assertions.assertThat(actualHealthAfterDelay).isNotNull();
    Assertions.assertThat(actualHealthAfterDelay.getStatus()).isEqualTo(Status.DOWN);
  }

  @Test
  public void
      shouldReportHealthStatusUpWhenBackendHealthIndicatorGoesDownTemporarilyButComesUpBeforeTheMaxDowntimeExpired()
          throws InterruptedException {
    // given
    final var sutDelayedHealthIndicator =
        new DelayedHealthIndicator(mockHealthIndicator, TEST_MAX_DOWNTIME);
    // backend health indicator was up in the past
    Mockito.when(mockHealthIndicator.health()).thenReturn(Health.up().build());
    sutDelayedHealthIndicator.checkHealth();

    // when
    // backend health indicator goes down
    Mockito.when(mockHealthIndicator.health()).thenReturn(Health.down().build());
    sutDelayedHealthIndicator.checkHealth();
    final Health actualHealthImmediate = sutDelayedHealthIndicator.health();

    // backend health indicator is up again
    Mockito.when(mockHealthIndicator.health()).thenReturn(Health.up().build());
    sutDelayedHealthIndicator.checkHealth();

    // wait for more then the configured max downtime
    Thread.sleep(100);

    final Health actualHealthAfterDelay = sutDelayedHealthIndicator.health();

    // then
    // immediate health report was up
    Assertions.assertThat(actualHealthImmediate).isNotNull();
    Assertions.assertThat(actualHealthImmediate.getStatus()).isEqualTo(Status.UP);

    // delayed health report is also up
    Assertions.assertThat(actualHealthAfterDelay).isNotNull();
    Assertions.assertThat(actualHealthAfterDelay.getStatus()).isEqualTo(Status.UP);
  }
}
