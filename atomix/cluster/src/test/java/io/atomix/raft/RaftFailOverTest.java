/*
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.raft;

import static org.assertj.core.api.Assertions.assertThat;

import io.atomix.storage.journal.Indexed;
import java.util.List;
import java.util.Map;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class RaftFailOverTest {

  @Rule @Parameter public RaftRule raftRule;

  @Parameters(name = "{index}: {0}")
  public static Object[][] raftConfigurations() {
    return new Object[][] {
      new Object[] {RaftRule.withBootstrappedNodes(3)},
      new Object[] {RaftRule.withBootstrappedNodes(4)},
      new Object[] {RaftRule.withBootstrappedNodes(5)}
    };
  }

  @Test
  public void shouldCommitEntriesAfterFollowerShutdown() throws Throwable {
    // given
    final var entryCount = 20;
    raftRule.appendEntries(entryCount);
    raftRule.awaitCommit(entryCount);
    raftRule.shutdownFollower();

    // when
    raftRule.appendEntries(entryCount);

    // then
    // 40 zeebe entries and 1 initial entry
    final var expectedEntryCount = entryCount * 2 + 1;
    raftRule.awaitCommit(expectedEntryCount);
    raftRule.awaitSameLogSizeOnAllNodes();
    final var memberLog = raftRule.getMemberLogs();

    final var logLength = memberLog.values().stream().map(List::size).findFirst().orElseThrow();
    assertThat(logLength).isEqualTo(expectedEntryCount);
    assertMemberLogs(memberLog);
  }

  @Test
  public void shouldCommitEntriesAfterLeaderShutdown() throws Throwable {
    // given
    final var entryCount = 20;
    raftRule.appendEntries(entryCount);
    raftRule.awaitCommit(entryCount);
    raftRule.shutdownLeader();

    // when
    raftRule.awaitNewLeader();
    raftRule.appendEntries(entryCount);

    // then
    // 40 zeebe entries and 2 initial entries
    final var expectedEntryCount = entryCount * 2 + 2;
    raftRule.awaitCommit(expectedEntryCount);
    raftRule.awaitSameLogSizeOnAllNodes();
    final var memberLog = raftRule.getMemberLogs();

    final var logLength = memberLog.values().stream().map(List::size).findFirst().orElseThrow();
    assertThat(logLength).isEqualTo(expectedEntryCount);
    assertMemberLogs(memberLog);
  }

  @Test
  public void shouldRecoverLeaderRestart() throws Throwable {
    // given
    final var entryCount = 20;
    raftRule.appendEntries(entryCount);
    raftRule.awaitCommit(entryCount);
    raftRule.restartLeader();

    // when
    raftRule.awaitNewLeader();
    raftRule.appendEntries(entryCount);

    // then
    // 40 zeebe entries and 2 initial entries
    final var expectedEntryCount = entryCount * 2 + 2;
    raftRule.awaitCommit(expectedEntryCount);
    raftRule.awaitSameLogSizeOnAllNodes();
    final var memberLog = raftRule.getMemberLogs();

    final var logLength = memberLog.values().stream().map(List::size).findFirst().orElseThrow();
    assertThat(logLength).isEqualTo(expectedEntryCount);
    assertMemberLogs(memberLog);
  }

  @Test
  @Ignore("https://github.com/zeebe-io/zeebe/issues/4467")
  public void testNodeCatchUpAfterCompaction() throws Exception {
    // given
    raftRule.shutdownServer("1");
    raftRule.awaitNewLeader();
    raftRule.appendEntries(100);
    raftRule.tryToCompactLogsOnServersExcept("1", 100).join();

    // when
    final var future = raftRule.startServer("1");

    // then
    future.join();
  }

  private void assertMemberLogs(final Map<String, List<Indexed<?>>> memberLog) {
    final var members = memberLog.keySet();
    final var iterator = members.iterator();

    if (iterator.hasNext()) {
      final var first = iterator.next();
      final var firstMemberEntries = memberLog.get(first);

      while (iterator.hasNext()) {
        final var otherEntries = memberLog.get(iterator.next());
        assertThat(firstMemberEntries)
            .withFailMessage(memberLog.toString())
            .containsExactly(otherEntries.toArray(new Indexed[0]));
      }
    }
  }
}
