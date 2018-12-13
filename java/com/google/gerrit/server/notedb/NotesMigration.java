// Copyright (C) 2017 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gerrit.server.notedb;

import com.google.inject.Singleton;
import java.util.Objects;

/**
 * Current low-level settings of the NoteDb migration for changes.
 *
 * <p>This class is a stub and will be removed soon; NoteDb is the only mode.
 */
@Singleton
public class NotesMigration {
  public static final String SECTION_NOTE_DB = "noteDb";
  public static final String READ = "read";
  public static final String WRITE = "write";
  public static final String DISABLE_REVIEW_DB = "disableReviewDb";
  public static final String PRIMARY_STORAGE = "primaryStorage";
  public static final String SEQUENCE = "sequence";

  /**
   * Read changes from NoteDb.
   *
   * <p>Change data is read from NoteDb refs, but ReviewDb is still the source of truth. If the
   * loader determines NoteDb is out of date, the change data in NoteDb will be transparently
   * rebuilt. This means that some code paths that look read-only may in fact attempt to write.
   *
   * <p>If true and {@code writeChanges() = false}, changes can still be read from NoteDb, but any
   * attempts to write will generate an error.
   */
  public final boolean readChanges() {
    return true;
  }

  /**
   * Read sequential change ID numbers from NoteDb.
   *
   * <p>If true, change IDs are read from {@code refs/sequences/changes} in All-Projects. If false,
   * change IDs are read from ReviewDb's native sequences.
   */
  public final boolean readChangeSequence() {
    return true;
  }

  /**
   * Disable ReviewDb access for changes.
   *
   * <p>When set, ReviewDb operations involving the Changes table become no-ops. Lookups return no
   * results; updates do nothing, as does opening, committing, or rolling back a transaction on the
   * Changes table.
   */
  public final boolean disableChangeReviewDb() {
    return true;
  }

  public final boolean commitChangeWrites() {
    return true;
  }

  public final boolean failChangeWrites() {
    return false;
  }

  @Override
  public final boolean equals(Object o) {
    return o instanceof NotesMigration;
  }

  @Override
  public final int hashCode() {
    return Objects.hash();
  }

  public NotesMigration() {}
}
