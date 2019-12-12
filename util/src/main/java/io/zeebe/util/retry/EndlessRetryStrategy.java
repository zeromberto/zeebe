/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.util.retry;

import io.zeebe.util.sched.ActorControl;
import io.zeebe.util.sched.future.ActorFuture;
import io.zeebe.util.sched.future.CompletableActorFuture;
import java.util.function.BooleanSupplier;

public class EndlessRetryStrategy implements RetryStrategy {

  private final ActorControl actor;
  private final ActorRetryMechanism retryMechanism;
  private CompletableActorFuture<Boolean> currentFuture;
  private BooleanSupplier terminateCondition;

  public EndlessRetryStrategy(final ActorControl actor) {
    this.actor = actor;
    this.retryMechanism = new ActorRetryMechanism(actor);
  }

  @Override
  public ActorFuture<Boolean> runWithRetry(final OperationToRetry callable) {
    return runWithRetry(callable, () -> false);
  }

  @Override
  public ActorFuture<Boolean> runWithRetry(
      final OperationToRetry callable, final BooleanSupplier condition) {
    currentFuture = new CompletableActorFuture<>();
    terminateCondition = condition;
    retryMechanism.wrap(callable, terminateCondition, currentFuture);

    actor.runUntilDone(this::run);

    return currentFuture;
  }

  private void run() {
    try {
      retryMechanism.run();
    } catch (final Exception exception) {
      if (terminateCondition.getAsBoolean()) {
        currentFuture.complete(false);
        actor.done();
      } else {
        actor.yield();
      }
    }
  }
}
