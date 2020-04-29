/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.broker.it.client.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import io.zeebe.broker.it.util.GrpcClientRule;
import io.zeebe.broker.test.EmbeddedBrokerRule;
import io.zeebe.client.ZeebeClient;
import io.zeebe.client.api.ZeebeFuture;
import io.zeebe.client.api.response.ActivateJobsResponse;
import io.zeebe.client.api.response.ActivatedJob;
import io.zeebe.test.util.BrokerClassRuleHelper;
import io.zeebe.util.ByteValue;
import java.util.Collection;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public final class ActivateJobsTest {

  @Rule public final BrokerClassRuleHelper helper = new BrokerClassRuleHelper();

  @Parameter(0)
  public String testName;

  @Parameter(1)
  public boolean longPollingEnabled;

  private final EmbeddedBrokerRule brokerRule =
      new EmbeddedBrokerRule(
          cfg -> cfg.getGateway().getLongPolling().setEnabled(longPollingEnabled));
  private final GrpcClientRule clientRule = new GrpcClientRule(brokerRule);
  @Rule public RuleChain ruleChain = RuleChain.outerRule(brokerRule).around(clientRule);
  private String jobType;

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return List.of(
        new Object[] {"longPolling disabled", false}, new Object[] {"longPolling enabled", true});
  }

  @Before
  public void init() {
    jobType = helper.getJobType();
  }

  @Test
  public void shouldActivateJobsRespectingAmountLimit() {
    // given
    final int availableJobs = 3;
    final int activateJobs = 2;

    clientRule.createJobs(jobType, availableJobs);

    // when
    final ActivateJobsResponse response =
        clientRule
            .getClient()
            .newActivateJobsCommand()
            .jobType(jobType)
            .maxJobsToActivate(activateJobs)
            .send()
            .join();

    // then
    assertThat(response.getJobs()).hasSize(activateJobs);
  }

  @Test
  public void shouldActivateJobsIfBatchIsTruncated() {
    // given
    final int availableJobs = 10;

    final ByteValue maxMessageSize = brokerRule.getBrokerCfg().getNetwork().getMaxMessageSize();
    final var largeVariableValue = "x".repeat((int) maxMessageSize.toBytes() / 4);
    final String variablesJson = String.format("{\"variablesJson\":\"%s\"}", largeVariableValue);

    clientRule.createJobs(jobType, b -> {}, variablesJson, availableJobs);

    // when
    final var response =
        clientRule
            .getClient()
            .newActivateJobsCommand()
            .jobType(jobType)
            .maxJobsToActivate(availableJobs)
            .send()
            .join();

    // then
    assertThat(response.getJobs()).hasSize(availableJobs);
  }

  @Test
  public void shouldWaitUntilJobsAvailable() {
    // assumed
    assumeThat(longPollingEnabled).isTrue();

    // given
    final int expectedJobsCount = 1;

    final ZeebeFuture<ActivateJobsResponse> responseFuture =
        clientRule
            .getClient()
            .newActivateJobsCommand()
            .jobType(jobType)
            .maxJobsToActivate(expectedJobsCount)
            .send();

    // when
    clientRule.createSingleJob(jobType);

    // then
    final ActivateJobsResponse response = responseFuture.join();
    assertThat(response.getJobs()).hasSize(expectedJobsCount);
  }

  @Test
  public void shouldActivatedJobForOpenRequest() throws InterruptedException {
    // assumed
    assumeThat(longPollingEnabled).isTrue();

    // given
    sendActivateRequestsAndClose(jobType, 3);

    final var activateJobsResponse =
        clientRule
            .getClient()
            .newActivateJobsCommand()
            .jobType(jobType)
            .maxJobsToActivate(5)
            .workerName("open")
            .send();

    sendActivateRequestsAndClose(jobType, 3);

    // when
    clientRule.createSingleJob(jobType);

    // then
    final var jobs = activateJobsResponse.join().getJobs();

    assertThat(jobs).hasSize(1).extracting(ActivatedJob::getWorker).contains("open");
  }

  private void sendActivateRequestsAndClose(final String jobType, final int count)
      throws InterruptedException {
    for (int i = 0; i < count; i++) {
      final ZeebeClient client =
          ZeebeClient.newClientBuilder()
              .brokerContactPoint(brokerRule.getGatewayAddress().toString())
              .usePlaintext()
              .build();

      client
          .newActivateJobsCommand()
          .jobType(jobType)
          .maxJobsToActivate(5)
          .workerName("closed-" + i)
          .send();

      Thread.sleep(100);
      client.close();
    }
  }
}
