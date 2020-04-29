/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.gateway.impl.configuration;

import static io.zeebe.gateway.impl.configuration.ConfigurationDefaults.DEFAULT_LONGPOLLING_ENABLED;
import static io.zeebe.gateway.impl.configuration.EnvironmentConstants.ENV_GATEWAY_LONGPOLLING_ENABLED;

import io.zeebe.util.Environment;
import java.util.Objects;

public final class LongPollingCfg {
  private boolean enabled = DEFAULT_LONGPOLLING_ENABLED;

  public void init() {
    init(new Environment());
  }

  public void init(final Environment environment) {
    environment.getBool(ENV_GATEWAY_LONGPOLLING_ENABLED).ifPresent(this::setEnabled);
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public int hashCode() {
    return Objects.hash(isEnabled());
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }

    if (other == null || getClass() != other.getClass()) {
      return false;
    }

    final LongPollingCfg that = (LongPollingCfg) other;
    return isEnabled() == that.isEnabled();
  }

  @Override
  public String toString() {
    return "LongPollingCfg{" + "enabled=" + enabled + '}';
  }
}
