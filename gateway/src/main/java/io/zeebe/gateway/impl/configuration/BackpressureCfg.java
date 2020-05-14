/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.gateway.impl.configuration;

import io.zeebe.util.Environment;
import java.util.Objects;

public final class BackpressureCfg {

  private boolean enabled = true;
  private final AIMDCfg aimd = new AIMDCfg();

  public void init(final Environment environment, final GatewayCfg gatewayCfg) {
    environment
        .getBool(EnvironmentConstants.ENV_GATEWAY_BACKPRESSURE_ENABLED)
        .ifPresent(this::setEnabled);
    aimd.init(environment, gatewayCfg);
  }

  public boolean isEnabled() {
    return enabled;
  }

  public BackpressureCfg setEnabled(final boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  public LimitAlgorithm getAlgorithm() {
    return LimitAlgorithm.AIMD;
  }

  public AIMDCfg getAimdCfg() {
    return aimd;
  }

  public enum LimitAlgorithm {
    AIMD,
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final BackpressureCfg that = (BackpressureCfg) o;
    return enabled == that.enabled && Objects.equals(aimd, that.aimd);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, aimd);
  }

  @Override
  public String toString() {
    return "BackpressureCfg{" + "enabled=" + enabled + ", aimd=" + aimd + '}';
  }
}
