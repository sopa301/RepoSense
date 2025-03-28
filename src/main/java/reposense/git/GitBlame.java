package reposense.git;

import static reposense.system.CommandRunner.runCommand;
import static reposense.util.StringsUtil.addQuotesForFilePath;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import reposense.git.model.GitBlameLineInfo;
import reposense.util.StringsUtil;

/**
 * Contains git blame related functionalities.
 * Git blame is responsible for showing which revision and author last modified each line of a file.
 */
public class GitBlame {
    public static final String IGNORE_COMMIT_LIST_FILE_NAME = ".git-blame-ignore-revs";

    private static final int BLAME_LINE_INFO_ROW_COUNT = 5;

    private static final String COMMIT_HASH_REGEX = "(^[0-9a-f]{40} .*)";
    private static final String AUTHOR_NAME_REGEX = "(^author .*)";
    private static final String AUTHOR_EMAIL_REGEX = "(^author-mail .*)";
    private static final String AUTHOR_TIME_REGEX = "(^author-time [0-9]+)";
    private static final String AUTHOR_TIMEZONE_REGEX = "(^author-tz .*)";
    private static final String COMMIT_TIME_REGEX = "(^committer-time .*)";
    private static final String COMBINATION_REGEX = COMMIT_HASH_REGEX + "|" + AUTHOR_NAME_REGEX + "|"
            + AUTHOR_EMAIL_REGEX + "|" + AUTHOR_TIME_REGEX + "|" + AUTHOR_TIMEZONE_REGEX;
    private static final String COMBINATION_WITH_COMMIT_TIME_REGEX = COMBINATION_REGEX + "|" + COMMIT_TIME_REGEX;

    private static final int AUTHOR_NAME_OFFSET = "author ".length();
    private static final int AUTHOR_EMAIL_OFFSET = "author-mail ".length();
    private static final int AUTHOR_TIME_OFFSET = "author-time ".length();
    private static final int COMMIT_TIME_OFFSET = "committer-time ".length();
    private static final int FULL_COMMIT_HASH_LENGTH = 40;

    /**
     * Returns the raw git blame result for the {@code fileDirectory}, performed at the {@code root} directory.
     */
    public static String blame(String root, String fileDirectory) {
        Path rootPath = Paths.get(root);

        String blameCommand = "git blame -w --line-porcelain";
        blameCommand += " " + addQuotesForFilePath(fileDirectory);

        return StringsUtil.filterText(runCommand(rootPath, blameCommand), COMBINATION_REGEX);
    }

    /**
     * Returns the raw git blame result with finding previous authors enabled for the {@code fileDirectory},
     * performed at the {@code root} directory.
     */
    public static String blameWithPreviousAuthors(String root, String fileDirectory) {
        Path rootPath = Paths.get(root);

        String blameCommandWithFindingPreviousAuthors = "git blame -w --line-porcelain --ignore-revs-file";
        blameCommandWithFindingPreviousAuthors += " " + addQuotesForFilePath(IGNORE_COMMIT_LIST_FILE_NAME);
        blameCommandWithFindingPreviousAuthors += " " + addQuotesForFilePath(fileDirectory);

        return StringsUtil.filterText(runCommand(rootPath, blameCommandWithFindingPreviousAuthors), COMBINATION_REGEX);
    }

    /**
     * Returns the processed git blame result for the {@code fileDirectory} performed at the {@code root} directory,
     * with reference to {@code withPreviousAuthors}.
     */
    public static List<GitBlameLineInfo> blameFile(String root, String fileDirectory, boolean withPreviousAuthors) {
        String blameResults = withPreviousAuthors
                ? blameWithPreviousAuthors(root, fileDirectory)
                : blame(root, fileDirectory);
        return processGitBlameResultLines(blameResults);
    }

    /**
     * Returns the git blame result for {@code lineNumber} of {@code fileDirectory} at {@code commitHash}.
     */
    public static GitBlameLineInfo blameLine(String root, String commitHash, String fileDirectory, int lineNumber) {
        Path rootPath = Paths.get(root);

        String blameCommand = String.format("git blame -w --line-porcelain %s -L %d,+1 -- %s",
                commitHash, lineNumber, fileDirectory);

        String blameResult = StringsUtil.filterText(runCommand(rootPath, blameCommand),
                COMBINATION_WITH_COMMIT_TIME_REGEX);
        String[] blameResultLines = StringsUtil.NEWLINE.split(blameResult);
        return processGitBlameResultLine(blameResultLines, false);
    }

    /**
     * Returns the processed result of {@code blameResults}.
     */
    private static List<GitBlameLineInfo> processGitBlameResultLines(String blameResults) {
        String[] blameResultsLines = StringsUtil.NEWLINE.split(blameResults);
        List<GitBlameLineInfo> blameFileResult = new ArrayList<>();
        for (int lineCount = 0; lineCount < blameResultsLines.length; lineCount += BLAME_LINE_INFO_ROW_COUNT) {
            String[] blameResultLines = Arrays
                    .copyOfRange(blameResultsLines, lineCount, lineCount + BLAME_LINE_INFO_ROW_COUNT - 1);
            GitBlameLineInfo blameLineInfo = processGitBlameResultLine(blameResultLines, true);
            blameFileResult.add(blameLineInfo);
        }
        return blameFileResult;
    }

    /**
     * Returns the processed result of {@code blameResultLines}, with reference to {@code useAuthorTime}.
     * If {@code useAuthorTime} is false, committer-time will be used for the timestamp.
     */
    private static GitBlameLineInfo processGitBlameResultLine(String[] blameResultLines, boolean useAuthorTime) {
        String commitHash = blameResultLines[0].substring(0, FULL_COMMIT_HASH_LENGTH);
        String authorName = blameResultLines[1].substring(AUTHOR_NAME_OFFSET);
        String authorEmail = blameResultLines[2].substring(AUTHOR_EMAIL_OFFSET).replaceAll("[<>]", "");
        long timestampInSeconds;
        if (useAuthorTime) {
            timestampInSeconds = Long.parseLong(blameResultLines[3].substring(AUTHOR_TIME_OFFSET));
        } else {
            timestampInSeconds = Long.parseLong(blameResultLines[5].substring(COMMIT_TIME_OFFSET));
        }

        return new GitBlameLineInfo(commitHash, authorName, authorEmail, timestampInSeconds);
    }
}
