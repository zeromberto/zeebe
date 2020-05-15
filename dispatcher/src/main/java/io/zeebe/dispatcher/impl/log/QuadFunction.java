package io.zeebe.dispatcher.impl.log;

@FunctionalInterface
public interface QuadFunction<A, B, C, D> {
  void test(A entry, B raftIndex, C firstRecordIndex, D lastRecordIndex);
}
