package org.iatoki.judgels.sandalphon.programming;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.gabriel.FakeClientMessage;
import org.iatoki.judgels.gabriel.FakeSealtiel;
import org.iatoki.judgels.gabriel.GradingLanguage;
import org.iatoki.judgels.gabriel.GradingType;
import org.iatoki.judgels.gabriel.Verdict;
import org.iatoki.judgels.gabriel.blackbox.BlackBoxGradingRequest;
import org.iatoki.judgels.gabriel.graders.BatchGradingConfig;
import org.iatoki.judgels.sandalphon.SandalphonProperties;
import org.iatoki.judgels.sandalphon.models.daos.programming.interfaces.ProblemDao;
import org.iatoki.judgels.sandalphon.models.daos.programming.interfaces.SubmissionDao;
import org.iatoki.judgels.sandalphon.models.domains.programming.ProblemModel;
import org.iatoki.judgels.sandalphon.models.domains.programming.SubmissionModel;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ProblemServiceImpl implements ProblemService {

    private final ProblemDao dao;
    private final SubmissionDao submissionDao;
    private final FakeSealtiel sealtiel;

    public ProblemServiceImpl(ProblemDao dao, SubmissionDao submissionDao, FakeSealtiel sealtiel) {
        this.dao = dao;
        this.submissionDao = submissionDao;
        this.sealtiel = sealtiel;
    }

    @Override
    public final Problem findProblemById(long id) {
        ProblemModel problemRecord = dao.findById(id);
        return createProblemFromModel(problemRecord);
    }

    @Override
    public final void updateProblem(long id, String name, String additionalNote) {
        ProblemModel model = dao.findById(id);
        model.name = name;
        model.additionalNote = additionalNote;
        dao.edit(model, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public Page<Problem> pageProblem(long page, long pageSize, String sortBy, String order, String filterString) {
        long totalPage = dao.countByFilter(filterString);
        List<ProblemModel> problemRecords = dao.findByFilterAndSort(filterString, sortBy, order, page * pageSize, pageSize);

        List<Problem> problems = problemRecords
                .stream()
                .map(problemRecord -> createProblemFromModel(problemRecord))
                .collect(Collectors.toList());

        return new Page<>(problems, totalPage, page, pageSize);
    }

    @Override
    public Page<Submission> pageSubmission(long page, long pageSize, String sortBy, String order, String filterString) {
        long totalPage = submissionDao.countByFilter(filterString);
        List<SubmissionModel> submissionRecords = submissionDao.findByFilterAndSort(filterString, sortBy, order, page * pageSize, pageSize);

        List<Submission> submissions = submissionRecords
                .stream()
                .map(submissionRecord -> createSubmissionFromModel(submissionRecord))
                .collect(Collectors.toList());

        return new Page<>(submissions, totalPage, page, pageSize);
    }

    @Override
    public Problem createProblem(String name, GradingType gradingType, String additionalNote) {
        ProblemModel problemRecord = new ProblemModel(name, gradingType, additionalNote);
        dao.persist(problemRecord, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

        File problemsDir = SandalphonProperties.getInstance().getProblemsDir();
        File problemDir = new File(problemsDir, problemRecord.jid);
        File statementDir = new File(problemDir, "statement");
        File gradingDir = new File(problemDir, "grading");

        try {
            FileUtils.forceMkdir(problemDir);
            FileUtils.forceMkdir(gradingDir);
            FileUtils.forceMkdir(new File(problemDir, "statement"));
            FileUtils.forceMkdir(new File(gradingDir, "testdata"));
            FileUtils.writeStringToFile(new File(statementDir, "statement.html"), "Keren parah");

            String json;

            switch (gradingType) {
                case BATCH_SUBTASK:
                    json = new Gson().toJson(BatchGradingConfig.createDefault());
                    break;
                default:
                    throw new IllegalStateException();
            }

            FileUtils.writeStringToFile(new File(gradingDir, "config.json"), json);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create directory for problem!");
        }

        return createProblemFromModel(problemRecord);
    }

    @Override
    public String getStatement(long id) {
        ProblemModel problemRecord = dao.findById(id);
        File problemsDir = SandalphonProperties.getInstance().getProblemsDir();
        File problemDir = new File(problemsDir, problemRecord.jid);
        File statementDir = new File(problemDir, "statement");
        String statement;
        try {
            statement = FileUtils.readFileToString(new File(statementDir, "statement.html"));
        } catch (IOException e) {
            throw new RuntimeException("Cannot read statement!");
        }

        return statement;
    }

    @Override
    public String getGradingConfig(long id) {
        ProblemModel problemRecord = dao.findById(id);
        File problemsDir = SandalphonProperties.getInstance().getProblemsDir();
        File problemDir = new File(problemsDir, problemRecord.jid);
        File gradingDir = new File(problemDir, "grading");
        String json;
        try {
            json = FileUtils.readFileToString(new File(gradingDir, "config.json"));
        } catch (IOException e) {
            throw new RuntimeException("Cannot read grading!");
        }

        return json;
    }

    @Override
    public void updateStatement(long id, String statement) {
        ProblemModel problemRecord = dao.findById(id);
        File problemsDir = SandalphonProperties.getInstance().getProblemsDir();
        File problemDir = new File(problemsDir, problemRecord.jid);
        File statementDir = new File(problemDir, "statement");
        try {
            FileUtils.writeStringToFile(new File(statementDir, "statement.html"), statement);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write statement!");
        }
    }

    @Override
    public void uploadTestDataFile(long id, File file, String filename) {
        ProblemModel problemRecord = dao.findById(id);
        File problemsDir = SandalphonProperties.getInstance().getProblemsDir();
        File problemDir = new File(problemsDir, problemRecord.jid);
        File gradingDir = new File(problemDir, "grading");
        try {
            FileUtils.copyFile(file, new File(new File(gradingDir, "testdata"), filename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateGradingConfig(long id, String json) {
        ProblemModel problemRecord = dao.findById(id);
        File problemsDir = SandalphonProperties.getInstance().getProblemsDir();
        File problemDir = new File(problemsDir, problemRecord.jid);
        File gradingDir = new File(problemDir, "grading");
        try {
            FileUtils.writeStringToFile(new File(gradingDir, "config.json"), json);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write json!");
        }
    }

    @Override
    public List<File> getTestDataFiles(long id) {
        ProblemModel problemRecord = dao.findById(id);
        File problemsDir = SandalphonProperties.getInstance().getProblemsDir();
        File testDataDir = FileUtils.getFile(problemsDir, problemRecord.jid, "grading", "testdata");

        if (!testDataDir.isDirectory()) {
            return ImmutableList.of();
        }

        return Arrays.asList(testDataDir.listFiles());
    }

    @Override
    public File getTestDataFile(long id, String filename) {
        ProblemModel problemRecord = dao.findById(id);
        File problemsDir = SandalphonProperties.getInstance().getProblemsDir();
        return FileUtils.getFile(problemsDir, problemRecord.jid, "grading", "testdata", filename);
    }

    @Override
    public List<File> getHelperFiles(long id) {
        ProblemModel problemRecord = dao.findById(id);
        File problemsDir = SandalphonProperties.getInstance().getProblemsDir();
        File helpersDir = FileUtils.getFile(problemsDir, problemRecord.jid, "grading", "helpers");

        if (!helpersDir.isDirectory()) {
            return ImmutableList.of();
        }

        return Arrays.asList(helpersDir.listFiles());
    }

    @Override
    public List<File> getMediaFiles(long id) {
        ProblemModel problemRecord = dao.findById(id);
        File problemsDir = SandalphonProperties.getInstance().getProblemsDir();
        File mediaDir = FileUtils.getFile(problemsDir, problemRecord.jid, "media");

        if (!mediaDir.isDirectory()) {
            return ImmutableList.of();
        }

        return Arrays.asList(mediaDir.listFiles());
    }

    @Override
    public void submit(long id, Map<String, byte[]> sourceFiles) {
        ProblemModel problemRecord = dao.findById(id);

        SubmissionModel submissionRecord = new SubmissionModel();
        submissionRecord.problemJid = problemRecord.jid;
        submissionRecord.verdict = Verdict.PENDING;
        submissionRecord.score = 0;
        submissionRecord.message = "Waiting for grading";

        submissionDao.persist(submissionRecord, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

        BlackBoxGradingRequest request = new BlackBoxGradingRequest(submissionRecord.jid, problemRecord.jid, problemRecord.timeUpdate, problemRecord.gradingType, GradingLanguage.CPP, sourceFiles);

        FakeClientMessage message = new FakeClientMessage("SFDSFDS", "BlackBoxGradingRequest", new Gson().toJson(request));
        sealtiel.sendMessage(message);
    }
    
    private Problem createProblemFromModel(ProblemModel record) {
        return new Problem(record.id, record.jid, record.name, record.gradingType, record.additionalNote);
    }

    private Submission createSubmissionFromModel(SubmissionModel record) {
        return new Submission(record.id, record.jid, record.problemJid, record.userCreate, record.verdict, record.score, record.message, new String(record.details));
    }
}
