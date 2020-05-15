/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.logstreams.storage.atomix;

import io.atomix.storage.journal.Indexed;
import io.atomix.storage.journal.index.JournalIndex;
import io.atomix.storage.journal.index.Position;
import io.atomix.storage.journal.index.SparseJournalIndex;

public final class ZeebeIndexAdapter implements JournalIndex, ZeebeIndexMapping {

  private final SparseJournalIndex sparseJournalIndex;

  private ZeebeIndexAdapter(final int density) {
    sparseJournalIndex = new SparseJournalIndex(density);
  }

  public static ZeebeIndexAdapter ofDensity(final int density) {
    return new ZeebeIndexAdapter(density);
  }

  @Override
  public void index(final Indexed indexedEntry, final int position) {
    sparseJournalIndex.index(indexedEntry, position);
  }

  @Override
  public Position lookup(final long index) {
    return sparseJournalIndex.lookup(index);
  }

  @Override
  public void truncate(final long index) {
    sparseJournalIndex.truncate(index);
  }

  @Override
  public void compact(final long index) {
    sparseJournalIndex.compact(index);
  }

  @Override
  public long lookupPosition(final long position) {
    return position >> 8;
  }
}
