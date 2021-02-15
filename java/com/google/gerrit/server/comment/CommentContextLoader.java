// Copyright (C) 2020 The Android Open Source Project
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

package com.google.gerrit.server.comment;

import static com.google.gerrit.entities.Patch.COMMIT_MSG;
import static com.google.gerrit.entities.Patch.MERGE_LIST;
import static java.util.stream.Collectors.groupingBy;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.common.Nullable;
import com.google.gerrit.entities.Comment;
import com.google.gerrit.entities.CommentContext;
import com.google.gerrit.entities.Project;
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.extensions.common.ContextLineInfo;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.patch.ComparisonType;
import com.google.gerrit.server.patch.Text;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

/**
 * Computes the list of {@link ContextLineInfo} for a given comment, that is, the lines of the
 * source file surrounding and including the area where the comment was written.
 */
public class CommentContextLoader {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final GitRepositoryManager repoManager;
  private final Project.NameKey project;

  public interface Factory {
    CommentContextLoader create(Project.NameKey project);
  }

  @Inject
  CommentContextLoader(GitRepositoryManager repoManager, @Assisted Project.NameKey project) {
    this.repoManager = repoManager;
    this.project = project;
  }

  /**
   * Load the comment context for multiple contextInputs at once. This method will open the
   * repository and read the source files for all necessary contextInputs' file paths.
   *
   * @param contextInputs a list of contextInputs.
   * @return a Map where all entries consist of the input contextInputs and the values are their
   *     corresponding {@link CommentContext}.
   */
  public Map<ContextInput, CommentContext> getContext(Collection<ContextInput> contextInputs)
      throws IOException {
    ImmutableMap.Builder<ContextInput, CommentContext> result =
        ImmutableMap.builderWithExpectedSize(Iterables.size(contextInputs));

    // Group contextInputs by commit ID so that each commit is parsed only once
    Map<ObjectId, List<ContextInput>> commentsByCommitId =
        contextInputs.stream().collect(groupingBy(ContextInput::commitId));

    try (Repository repo = repoManager.openRepository(project);
        RevWalk rw = new RevWalk(repo)) {
      for (ObjectId commitId : commentsByCommitId.keySet()) {
        RevCommit commit = rw.parseCommit(commitId);
        for (ContextInput contextInput : commentsByCommitId.get(commitId)) {
          Optional<Range> range = getStartAndEndLines(contextInput);
          if (!range.isPresent()) {
            result.put(contextInput, CommentContext.empty());
            continue;
          }
          String filePath = contextInput.filePath();
          switch (filePath) {
            case COMMIT_MSG:
              result.put(
                  contextInput,
                  getContextForCommitMessage(
                      rw.getObjectReader(), commit, range.get(), contextInput.contextPadding()));
              break;
            case MERGE_LIST:
              result.put(
                  contextInput,
                  getContextForMergeList(
                      rw.getObjectReader(), commit, range.get(), contextInput.contextPadding()));
              break;
            default:
              result.put(
                  contextInput,
                  getContextForFilePath(
                      repo, rw, commit, filePath, range.get(), contextInput.contextPadding()));
          }
        }
      }
      return result.build();
    }
  }

  private CommentContext getContextForCommitMessage(
      ObjectReader reader, RevCommit commit, Range commentRange, int contextPadding)
      throws IOException {
    Text text = Text.forCommit(reader, commit);
    return createContext(text, commentRange, contextPadding);
  }

  private CommentContext getContextForMergeList(
      ObjectReader reader, RevCommit commit, Range commentRange, int contextPadding)
      throws IOException {
    ComparisonType cmp = ComparisonType.againstParent(1);
    Text text = Text.forMergeList(cmp, reader, commit);
    return createContext(text, commentRange, contextPadding);
  }

  private CommentContext getContextForFilePath(
      Repository repo,
      RevWalk rw,
      RevCommit commit,
      String filePath,
      Range commentRange,
      int contextPadding)
      throws IOException {
    // TODO(ghareeb): We can further group the comments by file paths to avoid opening
    // the same file multiple times.
    try (TreeWalk tw = TreeWalk.forPath(rw.getObjectReader(), filePath, commit.getTree())) {
      if (tw == null) {
        logger.atWarning().log(
            "Could not find path %s in the git tree of ID %s.", filePath, commit.getTree().getId());
        return CommentContext.empty();
      }
      ObjectId id = tw.getObjectId(0);
      Text src = new Text(repo.open(id, Constants.OBJ_BLOB));
      return createContext(src, commentRange, contextPadding);
    }
  }

  private static CommentContext createContext(Text src, Range commentRange, int contextPadding) {
    if (commentRange.start() < 1 || commentRange.end() - 1 > src.size()) {
      throw new StorageException(
          "Invalid comment range "
              + commentRange
              + ". Text only contains "
              + src.size()
              + " lines.");
    }
    commentRange = adjustRange(commentRange, contextPadding, src.size());
    ImmutableMap.Builder<Integer, String> context =
        ImmutableMap.builderWithExpectedSize(commentRange.end() - commentRange.start());
    for (int i = commentRange.start(); i < commentRange.end(); i++) {
      context.put(i, src.getString(i - 1));
    }
    return CommentContext.create(context.build());
  }

  /**
   * Adjust the {@code commentRange} parameter by adding {@code contextPadding} lines before and
   * after the comment range.
   */
  private static Range adjustRange(Range commentRange, int contextPadding, int fileLines) {
    int newStartLine = commentRange.start() - contextPadding;
    int newEndLine = commentRange.end() + contextPadding;
    return Range.create(Math.max(1, newStartLine), Math.min(fileLines + 1, newEndLine));
  }

  private static Optional<Range> getStartAndEndLines(ContextInput comment) {
    if (comment.range() != null) {
      return Optional.of(Range.create(comment.range().startLine, comment.range().endLine + 1));
    } else if (comment.lineNumber() > 0) {
      return Optional.of(Range.create(comment.lineNumber(), comment.lineNumber() + 1));
    }
    return Optional.empty();
  }

  @AutoValue
  abstract static class Range {
    static Range create(int start, int end) {
      return new AutoValue_CommentContextLoader_Range(start, end);
    }

    /** Start line of the comment (inclusive). */
    abstract int start();

    /** End line of the comment (exclusive). */
    abstract int end();

    /** Number of lines covered by this range. */
    int size() {
      return end() - start();
    }
  }

  /** This entity only contains comment fields needed to load the comment context. */
  @AutoValue
  abstract static class ContextInput {
    static ContextInput fromComment(Comment comment, int contextPadding) {
      return new AutoValue_CommentContextLoader_ContextInput.Builder()
          .commitId(comment.getCommitId())
          .filePath(comment.key.filename)
          .range(comment.range)
          .lineNumber(comment.lineNbr)
          .contextPadding(contextPadding)
          .build();
    }

    /** 20 bytes SHA-1 of the patchset commit containing the file where the comment is written. */
    abstract ObjectId commitId();

    /** File path where the comment is written. */
    abstract String filePath();

    /**
     * Position of the comment in the file (start line, start char, end line, end char). This field
     * can be null if the range is not available for this comment.
     */
    @Nullable
    abstract Comment.Range range();

    /**
     * The 1-based line number where the comment is written. A value 0 means that the line number is
     * not available for this comment.
     */
    abstract Integer lineNumber();

    /** Number of extra lines of context that should be added before and after the comment range. */
    abstract Integer contextPadding();

    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder commitId(ObjectId commitId);

      public abstract Builder filePath(String filePath);

      public abstract Builder range(@Nullable Comment.Range range);

      public abstract Builder lineNumber(Integer lineNumber);

      public abstract Builder contextPadding(Integer contextPadding);

      public abstract ContextInput build();
    }
  }
}