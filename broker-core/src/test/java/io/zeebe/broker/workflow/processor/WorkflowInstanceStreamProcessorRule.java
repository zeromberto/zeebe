/*
 * Zeebe Broker Core
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.zeebe.broker.workflow.processor;

import static io.zeebe.test.util.TestUtil.doRepeatedly;
import static io.zeebe.test.util.TestUtil.waitUntil;
import static io.zeebe.util.buffer.BufferUtil.wrapString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.zeebe.broker.clustering.base.topology.TopologyManager;
import io.zeebe.broker.job.data.JobRecord;
import io.zeebe.broker.logstreams.processor.TypedRecord;
import io.zeebe.broker.subscription.command.SubscriptionCommandSender;
import io.zeebe.broker.topic.StreamProcessorControl;
import io.zeebe.broker.util.StreamProcessorRule;
import io.zeebe.broker.workflow.data.WorkflowInstanceRecord;
import io.zeebe.broker.workflow.deployment.data.DeploymentRecord;
import io.zeebe.broker.workflow.deployment.data.ResourceType;
import io.zeebe.broker.workflow.state.WorkflowState;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.BpmnModelInstance;
import io.zeebe.model.bpmn.instance.Process;
import io.zeebe.protocol.intent.Intent;
import io.zeebe.protocol.intent.JobIntent;
import io.zeebe.protocol.intent.WorkflowInstanceIntent;
import io.zeebe.util.buffer.BufferUtil;
import java.io.ByteArrayOutputStream;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.Rule;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;

public class WorkflowInstanceStreamProcessorRule extends ExternalResource {

  @Rule public TemporaryFolder folder = new TemporaryFolder();
  private final StreamProcessorRule environmentRule;

  private SubscriptionCommandSender mockSubscriptionCommandSender;
  private TopologyManager mockTopologyManager;

  private StreamProcessorControl streamProcessor;
  private WorkflowState workflowState;

  public WorkflowInstanceStreamProcessorRule(StreamProcessorRule streamProcessorRule) {
    this.environmentRule = streamProcessorRule;
  }

  public SubscriptionCommandSender getMockSubscriptionCommandSender() {
    return mockSubscriptionCommandSender;
  }

  @Override
  protected void before() throws Throwable {
    mockSubscriptionCommandSender = mock(SubscriptionCommandSender.class);
    mockTopologyManager = mock(TopologyManager.class);
    workflowState = new WorkflowState();

    when(mockSubscriptionCommandSender.hasPartitionIds()).thenReturn(true);
    when(mockSubscriptionCommandSender.openMessageSubscription(anyLong(), anyLong(), any(), any()))
        .thenReturn(true);
    when(mockSubscriptionCommandSender.correlateMessageSubscription(
            anyInt(), anyLong(), anyLong(), any()))
        .thenReturn(true);

    streamProcessor =
        environmentRule.runStreamProcessor(
            env -> {
              final WorkflowInstanceStreamProcessor streamProcessor =
                  new WorkflowInstanceStreamProcessor(
                      workflowState, mockSubscriptionCommandSender, mockTopologyManager);

              return streamProcessor.createStreamProcessor(env);
            });
  }

  public StreamProcessorControl getStreamProcessor() {
    return streamProcessor;
  }

  @Override
  protected void after() {
    // TODO Auto-generated method stub
    super.after();
  }

  public void deploy(final BpmnModelInstance modelInstance) {
    final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    Bpmn.writeModelToStream(outStream, modelInstance);
    final DirectBuffer xmlBuffer = new UnsafeBuffer(outStream.toByteArray());

    final DeploymentRecord record = new DeploymentRecord();
    final DirectBuffer resourceName = wrapString("resourceName");

    final Process process = modelInstance.getModelElementsByType(Process.class).iterator().next();

    record
        .resources()
        .add()
        .setResource(xmlBuffer)
        .setResourceName(resourceName)
        .setResourceType(ResourceType.BPMN_XML);

    record
        .workflows()
        .add()
        .setKey(1)
        .setResourceName(resourceName)
        .setBpmnProcessId(BufferUtil.wrapString(process.getId()))
        .setVersion(1);

    workflowState.putDeployment(1, record);
  }

  public TypedRecord<WorkflowInstanceRecord> createWorkflowInstance(final String processId) {
    return createWorkflowInstance(processId, wrapString(""));
  }

  public TypedRecord<WorkflowInstanceRecord> createWorkflowInstance(
      final String processId, final DirectBuffer payload) {
    environmentRule.writeCommand(
        WorkflowInstanceIntent.CREATE,
        workflowInstanceRecord(BufferUtil.wrapString(processId), payload));
    final TypedRecord<WorkflowInstanceRecord> createdEvent =
        awaitAndGetFirstRecordInState(WorkflowInstanceIntent.CREATED);
    return createdEvent;
  }

  public void completeFirstJob() {
    final TypedRecord<JobRecord> createCommand = awaitAndGetFirstRecordInState(JobIntent.CREATE);

    final long jobKey = environmentRule.writeEvent(JobIntent.CREATED, createCommand.getValue());
    environmentRule.writeEvent(jobKey, JobIntent.COMPLETED, createCommand.getValue());
  }

  private static WorkflowInstanceRecord workflowInstanceRecord(
      final DirectBuffer processId, final DirectBuffer payload) {
    final WorkflowInstanceRecord record = new WorkflowInstanceRecord();

    record.setBpmnProcessId(processId);
    record.setPayload(payload);

    return record;
  }

  private TypedRecord<WorkflowInstanceRecord> awaitAndGetFirstRecordInState(
      final WorkflowInstanceIntent state) {
    awaitFirstRecordInState(state);
    return environmentRule
        .events()
        .onlyWorkflowInstanceRecords()
        .withIntent(state)
        .findFirst()
        .get();
  }

  private TypedRecord<JobRecord> awaitAndGetFirstRecordInState(final JobIntent state) {
    awaitFirstRecordInState(state);
    return environmentRule.events().onlyJobRecords().withIntent(state).findFirst().get();
  }

  private void awaitFirstRecordInState(final Intent state) {
    waitUntil(() -> environmentRule.events().withIntent(state).findFirst().isPresent());
  }

  public TypedRecord<WorkflowInstanceRecord> awaitElementInState(
      final String elementId, final WorkflowInstanceIntent intent) {
    final DirectBuffer elementIdAsBuffer = BufferUtil.wrapString(elementId);

    return doRepeatedly(
            () ->
                environmentRule
                    .events()
                    .onlyWorkflowInstanceRecords()
                    .withIntent(intent)
                    .filter(r -> elementIdAsBuffer.equals(r.getValue().getActivityId()))
                    .findFirst())
        .until(o -> o.isPresent())
        .get();
  }
}
