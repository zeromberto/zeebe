/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.engine.processor.workflow.deployment.model.validation;

import io.zeebe.el.ExpressionLanguage;
import io.zeebe.model.bpmn.instance.ConditionExpression;
import io.zeebe.model.bpmn.instance.zeebe.ZeebeCalledElement;
import io.zeebe.model.bpmn.instance.zeebe.ZeebeInput;
import io.zeebe.model.bpmn.instance.zeebe.ZeebeLoopCharacteristics;
import io.zeebe.model.bpmn.instance.zeebe.ZeebeOutput;
import io.zeebe.model.bpmn.instance.zeebe.ZeebeSubscription;
import io.zeebe.model.bpmn.instance.zeebe.ZeebeTaskDefinition;
import java.util.Collection;
import java.util.List;
import org.camunda.bpm.model.xml.validation.ModelElementValidator;

public final class ZeebeRuntimeValidators {

  public static final Collection<ModelElementValidator<?>> getValidators(
      final ExpressionLanguage expressionLanguage) {
    return List.of(
        ZeebeExpressionValidator.verifyThat(ZeebeInput.class)
            .hasValidNonStaticExpression("Validation of ZeebeInput.source", ZeebeInput::getSource)
            .hasValidPath("Validation of ZeebeInput.target", ZeebeInput::getTarget)
            .build(expressionLanguage),
        ZeebeExpressionValidator.verifyThat(ZeebeOutput.class)
            .hasValidNonStaticExpression("Validation of ZeebeOutput.source", ZeebeOutput::getSource)
            .hasValidPath("Validation of ZeebeOutput.target", ZeebeOutput::getTarget)
            .build(expressionLanguage),
        ZeebeExpressionValidator.verifyThat(ZeebeSubscription.class)
            .hasValidExpression(
                "Validation of ZeebeSubscription.corelationKey",
                ZeebeSubscription::getCorrelationKey)
            .build(expressionLanguage),
        ZeebeJsonPathValidator.verifyThat(ZeebeLoopCharacteristics.class)
            .hasValidPathExpression(ZeebeLoopCharacteristics::getInputCollection)
            .hasValidPathExpression(ZeebeLoopCharacteristics::getOutputElement)
            .build(),
        ZeebeExpressionValidator.verifyThat(ZeebeCalledElement.class)
            .hasValidExpression(
                "Validation of ZeebeCalledElement.childElement", ZeebeCalledElement::getProcessId)
            .build(expressionLanguage),
        ZeebeExpressionValidator.verifyThat(ConditionExpression.class)
            .hasValidExpression(
                "Validation of ConditionExpression.textContent",
                ConditionExpression::getTextContent)
            .build(expressionLanguage),
        ZeebeExpressionValidator.verifyThat(ZeebeTaskDefinition.class)
            .hasValidExpression(
                "Validation of ZeebeTaskDefinition.type", ZeebeTaskDefinition::getType)
            .hasValidExpression(
                "Validation of ZeebeTaskDefinition.retries", ZeebeTaskDefinition::getRetries)
            .build(expressionLanguage));
  }
}
