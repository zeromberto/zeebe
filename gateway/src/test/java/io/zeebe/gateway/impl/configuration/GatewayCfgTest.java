/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.gateway.impl.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import io.zeebe.test.util.TestConfigurationFactory;
import io.zeebe.util.Environment;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public final class GatewayCfgTest {

  private static final String DEFAULT_CFG_FILENAME = "/configuration/gateway.default.yaml";
  private static final GatewayCfg DEFAULT_CFG = new GatewayCfg();
  private static final String EMPTY_CFG_FILENAME = "/configuration/gateway.empty.yaml";
  private static final String CUSTOM_CFG_FILENAME = "/configuration/gateway.custom.yaml";
  private static final GatewayCfg CUSTOM_CFG = new GatewayCfg();

  static {
    DEFAULT_CFG.init();
    CUSTOM_CFG.init();
    CUSTOM_CFG.getNetwork().setHost("192.168.0.1").setPort(123);
    CUSTOM_CFG
        .getCluster()
        .setContactPoint("foobar:1234")
        .setRequestTimeout(Duration.ofHours(123))
        .setClusterName("testCluster")
        .setMemberId("testMember")
        .setHost("1.2.3.4")
        .setPort(12321);
    CUSTOM_CFG
        .getSecurity()
        .setEnabled(true)
        .setCertificateChainPath("certificateChainPath")
        .setPrivateKeyPath("privateKeyPath");
    CUSTOM_CFG.getMonitoring().setEnabled(true).setHost("monitoringHost").setPort(1234);
    CUSTOM_CFG.getThreads().setManagementThreads(100);
  }

  private final Map<String, String> environment = new HashMap<>();

  @Test
  public void configIsNotInitilazedByDefault() {
    // given
    final var gatewayCfg = new GatewayCfg();

    // when
    final boolean actual = gatewayCfg.isInitialized();

    // then
    assertThat(actual).isFalse();
  }

  @Test
  public void shouldByInitializedAfterInitWithoutParametersIsCalled() {
    // given
    final var gatewayCfg = new GatewayCfg();

    // when
    gatewayCfg.init();
    final boolean actual = gatewayCfg.isInitialized();

    // then
    assertThat(actual).isTrue();
  }

  @Test
  public void shouldByInitializedAfterInitWithParametersIsCalled() {
    // given
    final var gatewayCfg = new GatewayCfg();

    // when
    gatewayCfg.init("Lorem Ipsum");
    final boolean actual = gatewayCfg.isInitialized();

    // then
    assertThat(actual).isTrue();
  }

  @Test
  public void shouldHaveDefaultValues() {
    // when
    final GatewayCfg gatewayCfg = readDefaultConfig();

    // then
    assertThat(gatewayCfg).isEqualTo(DEFAULT_CFG);
  }

  @Test
  public void shouldLoadEmptyConfig() {
    // when
    final GatewayCfg gatewayCfg = readEmptyConfig();

    // then
    assertThat(gatewayCfg).isEqualTo(DEFAULT_CFG);
  }

  @Test
  public void shouldLoadCustomConfig() {
    // when
    final GatewayCfg gatewayCfg = readCustomConfig();

    // then
    assertThat(gatewayCfg).isEqualTo(CUSTOM_CFG);
  }

  @Test
  public void shouldUseEnvironmentVariables() {
    // given
    setEnv("zeebe.gateway.network.host", "zeebe");
    setEnv("zeebe.gateway.network.port", "5432");
    setEnv("zeebe.gateway.cluster.contactPoint", "broker:432");
    setEnv("zeebe.gateway.threads.managementThreads", "32");
    setEnv("zeebe.gateway.cluster.requestTimeout", Duration.ofMinutes(43).toString());
    setEnv("zeebe.gateway.cluster.clusterName", "envCluster");
    setEnv("zeebe.gateway.cluster.memberId", "envMember");
    setEnv("zeebe.gateway.cluster.host", "envHost");
    setEnv("zeebe.gateway.cluster.port", "12345");
    setEnv("zeebe.gateway.monitoring.enabled", "true");
    setEnv("zeebe.gateway.monitoring.host", "monitorHost");
    setEnv("zeebe.gateway.monitoring.port", "231");
    setEnv("zeebe.gateway.security.enabled", String.valueOf(false));
    setEnv(
        "zeebe.gateway.security.privateKeyPath",
        GatewayCfgTest.class
            .getClassLoader()
            .getResource("security/test-server.key.pem")
            .getPath());
    setEnv(
        "zeebe.gateway.security.certificateChainPath",
        GatewayCfgTest.class
            .getClassLoader()
            .getResource("security/test-chain.cert.pem")
            .getPath());
    setEnv("zeebe.gateway.network.minKeepAliveInterval", Duration.ofSeconds(30).toString()); //

    final GatewayCfg expected = new GatewayCfg();
    expected
        .getNetwork()
        .setHost("zeebe")
        .setPort(5432)
        .setMinKeepAliveInterval(Duration.ofSeconds(30));
    expected
        .getCluster()
        .setContactPoint("broker:432")
        .setRequestTimeout(Duration.ofMinutes(43))
        .setClusterName("envCluster")
        .setMemberId("envMember")
        .setHost("envHost")
        .setPort(12345);
    expected.getThreads().setManagementThreads(32);
    expected.getMonitoring().setEnabled(true).setHost("monitorHost").setPort(231);
    expected
        .getSecurity()
        .setEnabled(false)
        .setPrivateKeyPath(
            getClass().getClassLoader().getResource("security/test-server.key.pem").getPath())
        .setCertificateChainPath(
            getClass().getClassLoader().getResource("security/test-chain.cert.pem").getPath());

    // when
    final GatewayCfg gatewayCfg = readCustomConfig();

    // then
    assertThat(gatewayCfg).isEqualTo(expected);
  }

  @Test
  public void shoudldInitializeMonitoringCfgWhenInitIsCalled() {
    // given
    final GatewayCfg sutGatewayConfig = new GatewayCfg();

    // when
    sutGatewayConfig.init();

    // then
    final MonitoringCfg monitoringCfg = sutGatewayConfig.getMonitoring();

    assertThat(monitoringCfg.getHost())
        .describedAs("Default monitoring host")
        .isEqualTo(ConfigurationDefaults.DEFAULT_HOST);
  }

  private void setEnv(final String key, final String value) {
    environment.put(key, value);
  }

  private GatewayCfg readDefaultConfig() {
    return readConfig(DEFAULT_CFG_FILENAME);
  }

  private GatewayCfg readEmptyConfig() {
    return readConfig(EMPTY_CFG_FILENAME);
  }

  private GatewayCfg readCustomConfig() {
    return readConfig(CUSTOM_CFG_FILENAME);
  }

  private GatewayCfg readConfig(final String filename) {
    try (final InputStream inputStream = GatewayCfgTest.class.getResourceAsStream(filename)) {
      if (inputStream != null) {
        final GatewayCfg gatewayCfg =
            new TestConfigurationFactory()
                .create(new Environment(environment), "zeebe.gateway", filename, GatewayCfg.class);
        gatewayCfg.init();
        return gatewayCfg;
      } else {
        throw new AssertionError("Unable to find configuration file: " + filename);
      }
    } catch (final IOException e) {
      throw new AssertionError("Failed to read configuration from file: " + filename, e);
    }
  }
}
