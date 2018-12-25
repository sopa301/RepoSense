import java.nio.file.InvalidPathException;
import reposense.git.GitCheckout;
import reposense.git.GitDiff;
import reposense.git.GitRevList;
    private static final String FILE_DELETED_SYMBOL = "/dev/null";
    private static final String INVALID_FILE_PATH_MESSAGE_FORMAT = "Invalid file path %s provided, skipping this file.";
    private static final Pattern FILE_CHANGED_PATTERN = Pattern.compile("\n(\\+){3} b?/(?<filePath>.*)\n");
        logger.info("Extracting relevant file info from " + config.getLocation() + "...");
            GitCheckout.checkoutDate(config.getRepoRoot(), config.getBranch(), config.getUntilDate());
        String lastCommitHash = GitRevList.getCommitHashBeforeDate(
        String fullDiffResult = GitDiff.diffCommit(config.getRepoRoot(), lastCommitHash);
            Matcher filePathMatcher = FILE_CHANGED_PATTERN.matcher(fileDiffResult);

            // diff result does not have the markers to indicate that file has any line changes, skip it
            if (!filePathMatcher.find()) {
            String filePath = filePathMatcher.group(FILE_CHANGED_GROUP_NAME);

            // file is deleted, skip it as well
            if (filePath.equals(FILE_DELETED_SYMBOL)) {
                continue;
            }
                try {
                    FileInfo currentFileInfo = generateFileInfo(config.getRepoRoot(), filePath);
                    setLinesToTrack(currentFileInfo, fileDiffResult);
                    fileInfos.add(currentFileInfo);
                } catch (InvalidPathException ipe) {
                    logger.warning(String.format(INVALID_FILE_PATH_MESSAGE_FORMAT, filePath));
                }
                    try {
                        fileInfos.add(generateFileInfo(config.getRepoRoot(), relativePath));
                    } catch (InvalidPathException ipe) {
                        logger.warning(String.format(INVALID_FILE_PATH_MESSAGE_FORMAT, filePath));
                    }