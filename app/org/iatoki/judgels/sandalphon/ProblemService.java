package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.commons.FileInfo;
import org.iatoki.judgels.commons.GitCommit;
import org.iatoki.judgels.commons.Page;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ProblemService {

    Problem createProblem(ProblemType type, String name, String additionalNote, String initialLanguageCode) throws IOException;

    boolean problemExistsByJid(String problemJid);

    Problem findProblemById(long problemId) throws ProblemNotFoundException;

    Problem findProblemByJid(String problemJid);

    boolean isProblemPartnerByUserJid(String problemJid, String userJid);

    void createProblemPartner(long problemId, String userJid, ProblemPartnerConfig baseConfig, ProblemPartnerChildConfig childConfig);

    void updateProblemPartner(long problemPartnerId, ProblemPartnerConfig baseConfig, ProblemPartnerChildConfig childConfig);

    Page<ProblemPartner> pageProblemPartners(String problemJid, long pageIndex, long pageSize, String orderBy, String orderDir);

    ProblemPartner findProblemPartnerByProblemPartnerId(long problemPartnerId) throws ProblemPartnerNotFoundException;

    ProblemPartner findProblemPartnerByProblemJidAndPartnerJid(String problemJid, String partnerJid);

    void updateProblem(long problemId, String name, String additionalNote);

    Page<Problem> pageProblems(long pageIndex, long pageSize, String orderBy, String orderDir, String filterString, String userJid, boolean isAdmin);

    Map<String, StatementLanguageStatus> getAvailableLanguages(String userJid, String problemJid) throws IOException;

    void addLanguage(String userJid, String problemJid, String languageCode) throws IOException;

    void enableLanguage(String userJid, String problemJid, String languageCode) throws IOException;

    void disableLanguage(String userJid, String problemJid, String languageCode) throws IOException;

    void makeDefaultLanguage(String userJid, String problemJid, String languageCode) throws IOException;

    String getDefaultLanguage(String userJid, String problemJid) throws IOException;

    String getStatement(String userJid, String problemJid, String languageCode) throws IOException;

    void updateStatement(String userJid, long problemId, String languageCode, String statement) throws IOException;

    void uploadStatementMediaFile(String userJid, long problemId, File mediaFile, String filename) throws IOException;

    void uploadStatementMediaFileZipped(String userJid, long problemId, File mediaFileZipped) throws IOException;

    List<FileInfo> getStatementMediaFiles(String userJid, String problemJid);

    String getStatementMediaFileURL(String userJid, String problemJid, String filename);

    List<GitCommit> getVersions(String userJid, String problemJid);

    void initRepository(String userJid, String problemJid);

    boolean userCloneExists(String userJid, String problemJid);

    void createUserCloneIfNotExists(String userJid, String problemJid);

    boolean commitThenMergeUserClone(String userJid, String problemJid, String title, String description);

    boolean updateUserClone(String userJid, String problemJid);

    boolean pushUserClone(String userJid, String problemJid);

    boolean fetchUserClone(String userJid, String problemJid);

    void discardUserClone(String userJid, String problemJid) throws IOException;

    void restore(String problemJid, String hash);
}
