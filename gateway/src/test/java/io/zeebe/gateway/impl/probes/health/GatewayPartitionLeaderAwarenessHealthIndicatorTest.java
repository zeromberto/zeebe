/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.gateway.impl.probes.health;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.zeebe.gateway.impl.broker.cluster.BrokerClusterState;
import java.util.List;
import java.util.function.Supplier;
import org.junit.Test;
import org.springframework.boot.actuate.health.Status;

public class GatewayPartitionLeaderAwarenessHealthIndicatorTest {

  @Test
  public void shouldRejectNullInConstructor() {
    // when + then
    assertThatThrownBy(() -> new GatewayPartitionLeaderAwarenessHealthIndicator(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void shouldReportUnknownIfSupplierReturnsNull() {
    // given
    final Supplier<BrokerClusterState> stateSupplier = () -> null;
    final var sutHealthIndicator =
        new GatewayPartitionLeaderAwarenessHealthIndicator(stateSupplier);

    // when
    final var actualHealth = sutHealthIndicator.health();

    // then
    assertThat(actualHealth).isNotNull();
    assertThat(actualHealth.getStatus()).isEqualTo(Status.UNKNOWN);
  }

  @Test
  public void shouldReportUpIfAnyPartitionLeaderIsKnown() {
    // given
    final BrokerClusterState mockClusterState = mock(BrokerClusterState.class);
    when(mockClusterState.getPartitions()).thenReturn(List.of(1, 2));
    when(mockClusterState.getLeaderForPartition(1)).thenReturn(BrokerClusterState.NODE_ID_NULL);
    when(mockClusterState.getLeaderForPartition(2)).thenReturn(42);

    final Supplier<BrokerClusterState> stateSupplier = () -> mockClusterState;
    final var sutHealthIndicator =
        new GatewayPartitionLeaderAwarenessHealthIndicator(stateSupplier);

    // when
    final var actualHealth = sutHealthIndicator.health();

    // then
    assertThat(actualHealth).isNotNull();
    assertThat(actualHealth.getStatus()).isEqualTo(Status.UP);
  }

  @Test
  public void shouldReportDownIfListOfPartitionsIsEmpty() {
    // given
    final BrokerClusterState mockClusterState = mock(BrokerClusterState.class);
    when(mockClusterState.getPartitions()).thenReturn(emptyList());

    final Supplier<BrokerClusterState> stateSupplier = () -> mockClusterState;
    final var sutHealthIndicator =
        new GatewayPartitionLeaderAwarenessHealthIndicator(stateSupplier);

    // when
    final var actualHealth = sutHealthIndicator.health();

    // then
    assertThat(actualHealth).isNotNull();
    assertThat(actualHealth.getStatus()).isEqualTo(Status.DOWN);
  }

  @Test
  public void shouldReportDownIfNoPartitionLeaderIsKnown() {
    // given
    final BrokerClusterState mockClusterState = mock(BrokerClusterState.class);
    when(mockClusterState.getPartitions()).thenReturn(List.of(1, 2));
    when(mockClusterState.getLeaderForPartition(1)).thenReturn(BrokerClusterState.NODE_ID_NULL);
    when(mockClusterState.getLeaderForPartition(2)).thenReturn(BrokerClusterState.NODE_ID_NULL);

    final Supplier<BrokerClusterState> stateSupplier = () -> mockClusterState;
    final var sutHealthIndicator =
        new GatewayPartitionLeaderAwarenessHealthIndicator(stateSupplier);

    // when
    final var actualHealth = sutHealthIndicator.health();

    // then
    assertThat(actualHealth).isNotNull();
    assertThat(actualHealth.getStatus()).isEqualTo(Status.DOWN);
  }
}
