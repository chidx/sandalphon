package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.FileInfo;
import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.InternalLink;
import org.iatoki.judgels.play.LazyHtml;
import org.iatoki.judgels.play.controllers.AbstractJudgelsController;
import org.iatoki.judgels.play.views.html.layouts.subtabLayout;
import org.iatoki.judgels.gabriel.GradingConfig;
import org.iatoki.judgels.gabriel.GradingEngineRegistry;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemNotFoundException;
import org.iatoki.judgels.sandalphon.adapters.GradingEngineAdapterRegistry;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import org.iatoki.judgels.sandalphon.LanguageRestriction;
import org.iatoki.judgels.sandalphon.LanguageRestrictionAdapter;
import org.iatoki.judgels.sandalphon.controllers.securities.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.securities.HasRole;
import org.iatoki.judgels.sandalphon.controllers.securities.LoggedIn;
import org.iatoki.judgels.sandalphon.forms.UploadFileForm;
import org.iatoki.judgels.sandalphon.forms.GradingEngineEditForm;
import org.iatoki.judgels.sandalphon.forms.LanguageRestrictionEditForm;
import org.iatoki.judgels.sandalphon.adapters.GradingEngineAdapter;
import org.iatoki.judgels.sandalphon.services.ProgrammingProblemService;
import org.iatoki.judgels.sandalphon.adapters.ConfigurableWithAutoPopulation;
import org.iatoki.judgels.sandalphon.adapters.ConfigurableWithTokilibFormat;
import org.iatoki.judgels.sandalphon.views.html.problem.programming.grading.autoPopulationLayout;
import org.iatoki.judgels.sandalphon.views.html.problem.programming.grading.tokilibLayout;
import org.iatoki.judgels.sandalphon.views.html.problem.programming.grading.listGradingHelperFilesView;
import org.iatoki.judgels.sandalphon.views.html.problem.programming.grading.listGradingTestDataFilesView;
import org.iatoki.judgels.sandalphon.views.html.problem.programming.grading.editGradingEngineView;
import org.iatoki.judgels.sandalphon.views.html.problem.programming.grading.editLanguageRestrictionView;
import play.api.mvc.Call;
import play.data.Form;
import play.db.jpa.Transactional;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.RequireCSRFCheck;
import play.i18n.Messages;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

@Authenticated(value = {LoggedIn.class, HasRole.class})
@Singleton
@Named
public final class ProgrammingProblemGradingController extends AbstractJudgelsController {

    private final ProblemService problemService;
    private final ProgrammingProblemService programmingProblemService;

    @Inject
    public ProgrammingProblemGradingController(ProblemService problemService, ProgrammingProblemService programmingProblemService) {
        this.problemService = problemService;
        this.programmingProblemService = programmingProblemService;
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result editGradingEngine(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProgrammingProblemControllerUtils.isAllowedToManageGrading(problemService, problem)) {
            return notFound();
        }

        GradingEngineEditForm gradingEngineEditData = new GradingEngineEditForm();
        try  {
            gradingEngineEditData.gradingEngineName = programmingProblemService.getGradingEngine(IdentityUtils.getUserJid(), problem.getJid());
        } catch (IOException e) {
            gradingEngineEditData.gradingEngineName = GradingEngineRegistry.getInstance().getDefaultEngine();
        }

        Form<GradingEngineEditForm> gradingEngineEditForm = Form.form(GradingEngineEditForm.class).fill(gradingEngineEditData);

        SandalphonControllerUtils.getInstance().addActivityLog("Try to update grading engine of problem " + problem.getSlug() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return showEditGradingEngine(gradingEngineEditForm, problem);
    }

    @Transactional
    @RequireCSRFCheck
    public Result postEditGradingEngine(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProgrammingProblemControllerUtils.isAllowedToManageGrading(problemService, problem)) {
            return notFound();
        }

        Form<GradingEngineEditForm> gradingEngineEditForm = Form.form(GradingEngineEditForm.class).bindFromRequest(request());

        if (formHasErrors(gradingEngineEditForm)) {
            return showEditGradingEngine(gradingEngineEditForm, problem);
        }

        problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

        String gradingEngine = gradingEngineEditForm.get().gradingEngineName;
        String originalGradingEngine;
        try {
            originalGradingEngine = programmingProblemService.getGradingEngine(IdentityUtils.getUserJid(), problem.getJid());
        } catch (IOException e) {
            originalGradingEngine = GradingEngineRegistry.getInstance().getDefaultEngine();
        }

        try {
            if (!gradingEngine.equals(originalGradingEngine)) {
                GradingConfig config = GradingEngineRegistry.getInstance().getEngine(gradingEngine).createDefaultGradingConfig();
                programmingProblemService.updateGradingConfig(IdentityUtils.getUserJid(), problem.getJid(), config);
            }

            programmingProblemService.updateGradingEngine(IdentityUtils.getUserJid(), problem.getJid(), gradingEngine);
        } catch (IOException e) {
            gradingEngineEditForm.reject("problem.programming.grading.engine.error.cantUpdate");
            return showEditGradingEngine(gradingEngineEditForm, problem);
        }

        SandalphonControllerUtils.getInstance().addActivityLog("Update grading engine of problem " + problem.getSlug() + ".");

        return redirect(routes.ProgrammingProblemGradingController.editGradingConfig(problem.getId()));
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result editGradingConfig(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProgrammingProblemControllerUtils.isAllowedToManageGrading(problemService, problem)) {
            return notFound();
        }

        String engine;
        try {
            engine = programmingProblemService.getGradingEngine(IdentityUtils.getUserJid(), problem.getJid());
        } catch (IOException e) {
            engine = GradingEngineRegistry.getInstance().getDefaultEngine();
        }
        GradingConfig config;
        try {
            config = programmingProblemService.getGradingConfig(IdentityUtils.getUserJid(), problem.getJid());
        } catch (IOException e) {
            config = GradingEngineRegistry.getInstance().getEngine(engine).createDefaultGradingConfig();
        }
        List<FileInfo> testDataFiles = programmingProblemService.getGradingTestDataFiles(IdentityUtils.getUserJid(), problem.getJid());
        List<FileInfo> helperFiles = programmingProblemService.getGradingHelperFiles(IdentityUtils.getUserJid(), problem.getJid());

        Form<?> gradingEngineConfForm = GradingEngineAdapterRegistry.getInstance().getByGradingEngineName(engine).createFormFromConfig(config);

        SandalphonControllerUtils.getInstance().addActivityLog("Try to update grading config of problem " + problem.getSlug() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return showEditGradingConfig(gradingEngineConfForm, problem, engine, testDataFiles, helperFiles);
    }

    @Transactional
    @RequireCSRFCheck
    public Result postEditGradingConfig(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);
        if (!ProgrammingProblemControllerUtils.isAllowedToManageGrading(problemService, problem)) {
            return notFound();
        }

        String engine;
        try {
            engine = programmingProblemService.getGradingEngine(IdentityUtils.getUserJid(), problem.getJid());
        } catch (IOException e) {
            engine = GradingEngineRegistry.getInstance().getDefaultEngine();
        }
        Form<?> gradingEngineConfForm = GradingEngineAdapterRegistry.getInstance().getByGradingEngineName(engine).createEmptyForm().bindFromRequest(request());

        if (formHasErrors(gradingEngineConfForm)) {
            List<FileInfo> testDataFiles = programmingProblemService.getGradingTestDataFiles(IdentityUtils.getUserJid(), problem.getJid());
            List<FileInfo> helperFiles = programmingProblemService.getGradingHelperFiles(IdentityUtils.getUserJid(), problem.getJid());

            return showEditGradingConfig(gradingEngineConfForm, problem, engine, testDataFiles, helperFiles);
        }

        problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

        try {
            GradingConfig config = GradingEngineAdapterRegistry.getInstance().getByGradingEngineName(engine).createConfigFromForm(gradingEngineConfForm);
            programmingProblemService.updateGradingConfig(IdentityUtils.getUserJid(), problem.getJid(), config);
        } catch (IOException e) {
            gradingEngineConfForm.reject("problem.programming.grading.config.error.cantUpdate");
            List<FileInfo> testDataFiles = programmingProblemService.getGradingTestDataFiles(IdentityUtils.getUserJid(), problem.getJid());
            List<FileInfo> helperFiles = programmingProblemService.getGradingHelperFiles(IdentityUtils.getUserJid(), problem.getJid());

            return showEditGradingConfig(gradingEngineConfForm, problem, engine, testDataFiles, helperFiles);
        }

        SandalphonControllerUtils.getInstance().addActivityLog("Update grading config of problem " + problem.getSlug() + ".");

        return redirect(routes.ProgrammingProblemGradingController.editGradingConfig(problem.getId()));
    }

    @Transactional
    public Result editGradingConfigByTokilibFormat(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProgrammingProblemControllerUtils.isAllowedToManageGrading(problemService, problem)) {
            return notFound();
        }

        problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

        String engine;
        try {
            engine = programmingProblemService.getGradingEngine(IdentityUtils.getUserJid(), problem.getJid());
        } catch (IOException e) {
            engine = GradingEngineRegistry.getInstance().getDefaultEngine();
        }

        GradingEngineAdapter adapter = GradingEngineAdapterRegistry.getInstance().getByGradingEngineName(engine);

        if (!(adapter instanceof ConfigurableWithTokilibFormat)) {
            return forbidden();
        }

        List<FileInfo> testDataFiles = programmingProblemService.getGradingTestDataFiles(IdentityUtils.getUserJid(), problem.getJid());
        GradingConfig config;
        try {
            config = programmingProblemService.getGradingConfig(IdentityUtils.getUserJid(), problem.getJid());
        } catch (IOException e) {
            config = GradingEngineRegistry.getInstance().getEngine(engine).createDefaultGradingConfig();
        }

        try {
            GradingConfig newConfig = ((ConfigurableWithTokilibFormat) adapter).updateConfigWithTokilibFormat(config, testDataFiles);
            programmingProblemService.updateGradingConfig(IdentityUtils.getUserJid(), problem.getJid(), newConfig);
        } catch (IOException e) {
            throw new IllegalStateException("Can't update grading config using tokilib format", e);
        }

        SandalphonControllerUtils.getInstance().addActivityLog("Update grading config using tokilib format of problem " + problem.getSlug() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.ProgrammingProblemGradingController.editGradingConfig(problem.getId()));
    }

    @Transactional
    public Result editGradingConfigByAutoPopulation(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProgrammingProblemControllerUtils.isAllowedToManageGrading(problemService, problem)) {
            return notFound();
        }

        problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

        String engine;
        try {
            engine = programmingProblemService.getGradingEngine(IdentityUtils.getUserJid(), problem.getJid());
        } catch (IOException e) {
            engine = GradingEngineRegistry.getInstance().getDefaultEngine();
        }
        List<FileInfo> testDataFiles = programmingProblemService.getGradingTestDataFiles(IdentityUtils.getUserJid(), problem.getJid());
        GradingEngineAdapter adapter = GradingEngineAdapterRegistry.getInstance().getByGradingEngineName(engine);

        if (!(adapter instanceof ConfigurableWithAutoPopulation)) {
            return forbidden();
        }

        GradingConfig config;
        try {
            config = programmingProblemService.getGradingConfig(IdentityUtils.getUserJid(), problem.getJid());
        } catch (IOException e) {
            config = GradingEngineRegistry.getInstance().getEngine(engine).createDefaultGradingConfig();
        }
        GradingConfig newConfig = ((ConfigurableWithAutoPopulation) adapter).updateConfigWithAutoPopulation(config, testDataFiles);

        try {
            programmingProblemService.updateGradingConfig(IdentityUtils.getUserJid(), problem.getJid(), newConfig);
        } catch (IOException e) {
            throw new IllegalStateException("Can't update grading config using auto population", e);
        }

        SandalphonControllerUtils.getInstance().addActivityLog("Update grading config by auto population of problem " + problem.getSlug() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return redirect(routes.ProgrammingProblemGradingController.editGradingConfig(problem.getId()));
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result listGradingTestDataFiles(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProgrammingProblemControllerUtils.isAllowedToManageGrading(problemService, problem)) {
            return notFound();
        }

        Form<UploadFileForm> uploadFileForm = Form.form(UploadFileForm.class);
        List<FileInfo> testDataFiles = programmingProblemService.getGradingTestDataFiles(IdentityUtils.getUserJid(), problem.getJid());

        SandalphonControllerUtils.getInstance().addActivityLog("List grading test data files of problem " + problem.getSlug() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return showListGradingTestDataFiles(uploadFileForm, problem, testDataFiles);
    }

    @Transactional
    @RequireCSRFCheck
    public Result postUploadGradingTestDataFiles(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProgrammingProblemControllerUtils.isAllowedToManageGrading(problemService, problem)) {
            return notFound();
        }

        Http.MultipartFormData body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart file;

        file = body.getFile("file");
        if (file != null) {
            problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

            File testDataFile = file.getFile();
            try {
                programmingProblemService.uploadGradingTestDataFile(IdentityUtils.getUserJid(), problem.getJid(), testDataFile, file.getFilename());
            } catch (IOException e) {
                Form<UploadFileForm> form = Form.form(UploadFileForm.class);
                List<FileInfo> testDataFiles = programmingProblemService.getGradingTestDataFiles(IdentityUtils.getUserJid(), problem.getJid());
                form.reject("problem.programming.grading.error.cantUploadTestData");

                return showListGradingTestDataFiles(form, problem, testDataFiles);
            }

            SandalphonControllerUtils.getInstance().addActivityLog("Upload test data file of problem " + problem.getSlug() + ".");

            return redirect(routes.ProgrammingProblemGradingController.listGradingTestDataFiles(problem.getId()));
        }

        file = body.getFile("fileZipped");
        if (file != null) {
            problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

            File testDataFile = file.getFile();
            try {
                programmingProblemService.uploadGradingTestDataFileZipped(IdentityUtils.getUserJid(), problem.getJid(), testDataFile);
            } catch (IOException e) {
                Form<UploadFileForm> form = Form.form(UploadFileForm.class);
                List<FileInfo> testDataFiles = programmingProblemService.getGradingTestDataFiles(IdentityUtils.getUserJid(), problem.getJid());
                form.reject("problem.programming.grading.error.cantUploadTestDataZipped");

                return showListGradingTestDataFiles(form, problem, testDataFiles);
            }

            SandalphonControllerUtils.getInstance().addActivityLog("Upload zipped test data files of problem " + problem.getSlug() + ".");

            return redirect(routes.ProgrammingProblemGradingController.listGradingTestDataFiles(problem.getId()));
        }

        return redirect(routes.ProgrammingProblemGradingController.listGradingTestDataFiles(problem.getId()));
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result listGradingHelperFiles(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProgrammingProblemControllerUtils.isAllowedToManageGrading(problemService, problem)) {
            return notFound();
        }

        Form<UploadFileForm> uploadFileForm = Form.form(UploadFileForm.class);
        List<FileInfo> helperFiles = programmingProblemService.getGradingHelperFiles(IdentityUtils.getUserJid(), problem.getJid());

        SandalphonControllerUtils.getInstance().addActivityLog("List grading helper files of problem " + problem.getSlug() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return showListGradingHelperFiles(uploadFileForm, problem, helperFiles);
    }

    @Transactional
    @RequireCSRFCheck
    public Result postUploadGradingHelperFiles(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProgrammingProblemControllerUtils.isAllowedToManageGrading(problemService, problem)) {
            return notFound();
        }

        Http.MultipartFormData body = request().body().asMultipartFormData();
        Http.MultipartFormData.FilePart file;

        file = body.getFile("file");
        if (file != null) {
            problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

            File helperFile = file.getFile();
            try {
                programmingProblemService.uploadGradingHelperFile(IdentityUtils.getUserJid(), problem.getJid(), helperFile, file.getFilename());
            } catch (IOException e) {
                Form<UploadFileForm> form = Form.form(UploadFileForm.class);
                List<FileInfo> helperFiles = programmingProblemService.getGradingHelperFiles(IdentityUtils.getUserJid(), problem.getJid());
                form.reject("problem.programming.grading.error.cantUploadHelper");

                return showListGradingHelperFiles(form, problem, helperFiles);
            }

            SandalphonControllerUtils.getInstance().addActivityLog("Upload helper file of problem " + problem.getSlug() + ".");

            return redirect(routes.ProgrammingProblemGradingController.listGradingHelperFiles(problem.getId()));
        }

        file = body.getFile("fileZipped");
        if (file != null) {
            problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

            File helperFile = file.getFile();
            try {
                programmingProblemService.uploadGradingHelperFileZipped(IdentityUtils.getUserJid(), problem.getJid(), helperFile);
            } catch (IOException e) {
                Form<UploadFileForm> form = Form.form(UploadFileForm.class);
                List<FileInfo> helperFiles = programmingProblemService.getGradingHelperFiles(IdentityUtils.getUserJid(), problem.getJid());
                form.reject("problem.programming.grading.error.cantUploadHelperZipped");

                return showListGradingHelperFiles(form, problem, helperFiles);
            }

            SandalphonControllerUtils.getInstance().addActivityLog("Upload zipped helper files of problem " + problem.getSlug() + ".");

            return redirect(routes.ProgrammingProblemGradingController.listGradingHelperFiles(problem.getId()));
        }

        return redirect(routes.ProgrammingProblemGradingController.listGradingHelperFiles(problem.getId()));
    }

    @Transactional(readOnly = true)
    public Result downloadGradingTestDataFile(long problemId, String filename) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProgrammingProblemControllerUtils.isAllowedToManageGrading(problemService, problem)) {
            return notFound();
        }

        String testDataURL = programmingProblemService.getGradingTestDataFileURL(IdentityUtils.getUserJid(), problem.getJid(), filename);

        SandalphonControllerUtils.getInstance().addActivityLog("Download test data file " + filename + " of problem " + problem.getSlug() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        try {
            new URL(testDataURL);
            return redirect(testDataURL);
        } catch (MalformedURLException e) {
            File testDataFile = new File(testDataURL);
            return ProblemControllerUtils.downloadFile(testDataFile);
        }
    }

    @Transactional(readOnly = true)
    public Result downloadGradingHelperFile(long problemId, String filename) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProgrammingProblemControllerUtils.isAllowedToManageGrading(problemService, problem)) {
            return notFound();
        }

        String helperURL = programmingProblemService.getGradingHelperFileURL(IdentityUtils.getUserJid(), problem.getJid(), filename);

        SandalphonControllerUtils.getInstance().addActivityLog("Download helper file " + filename + " of problem " + problem.getSlug() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        try {
            new URL(helperURL);
            return redirect(helperURL);
        } catch (MalformedURLException e) {
            File helperFile = new File(helperURL);
            return ProblemControllerUtils.downloadFile(helperFile);
        }
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result editLanguageRestriction(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProgrammingProblemControllerUtils.isAllowedToManageGrading(problemService, problem)) {
            return notFound();
        }

        LanguageRestriction languageRestriction;
        try {
            languageRestriction = programmingProblemService.getLanguageRestriction(IdentityUtils.getUserJid(), problem.getJid());
        } catch (IOException e) {
            languageRestriction = LanguageRestriction.defaultRestriction();
        }

        LanguageRestrictionEditForm languageRestrictionEditData = new LanguageRestrictionEditForm();
        languageRestrictionEditData.allowedLanguageNames = LanguageRestrictionAdapter.getFormAllowedLanguageNamesFromLanguageRestriction(languageRestriction);
        languageRestrictionEditData.isAllowedAll = LanguageRestrictionAdapter.getFormIsAllowedAllFromLanguageRestriction(languageRestriction);

        Form<LanguageRestrictionEditForm> languageRestrictionEditForm = Form.form(LanguageRestrictionEditForm.class).fill(languageRestrictionEditData);

        SandalphonControllerUtils.getInstance().addActivityLog("Try to update language restriction of problem " + problem.getSlug() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return showEditLanguageRestriction(languageRestrictionEditForm, problem);
    }

    @Transactional
    @RequireCSRFCheck
    public Result postEditLanguageRestriction(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (ProgrammingProblemControllerUtils.isAllowedToManageGrading(problemService, problem)) {
            return notFound();
        }

        Form<LanguageRestrictionEditForm> languageRestrictionEditForm = Form.form(LanguageRestrictionEditForm.class).bindFromRequest(request());

        if (formHasErrors(languageRestrictionEditForm)) {
            return showEditLanguageRestriction(languageRestrictionEditForm, problem);
        }

        problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

        LanguageRestrictionEditForm data = languageRestrictionEditForm.get();
        LanguageRestriction languageRestriction = LanguageRestrictionAdapter.createLanguageRestrictionFromForm(data.allowedLanguageNames, data.isAllowedAll);

        try {
            programmingProblemService.updateLanguageRestriction(IdentityUtils.getUserJid(), problem.getJid(), languageRestriction);
        } catch (IOException e) {
            languageRestrictionEditForm.reject("problem.programming.language.error.cantUpdate");
            return showEditLanguageRestriction(languageRestrictionEditForm, problem);
        }

        SandalphonControllerUtils.getInstance().addActivityLog("Update language restriction of problem " + problem.getSlug() + ".");

        return redirect(routes.ProgrammingProblemGradingController.editLanguageRestriction(problem.getId()));
    }

    private Result showEditGradingEngine(Form<GradingEngineEditForm> gradingEngineEditForm, Problem problem) {
        LazyHtml content = new LazyHtml(editGradingEngineView.render(gradingEngineEditForm, problem));
        appendSubtabsLayout(content, problem);
        ProgrammingProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        SandalphonControllerUtils.getInstance().appendSidebarLayout(content);
        appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.programming.grading.engine.update"), routes.ProgrammingProblemGradingController.editGradingEngine(problem.getId())));
        SandalphonControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Grading Engine");

        return SandalphonControllerUtils.getInstance().lazyOk(content);
    }

    private Result showEditGradingConfig(Form<?> gradingConfForm, Problem problem, String gradingEngine, List<FileInfo> testDataFiles, List<FileInfo> helperFiles) {
        GradingEngineAdapter adapter = GradingEngineAdapterRegistry.getInstance().getByGradingEngineName(gradingEngine);
        Call postUpdateGradingConfigCall = routes.ProgrammingProblemGradingController.postEditGradingConfig(problem.getId());
        LazyHtml content = new LazyHtml(adapter.renderUpdateGradingConfig(gradingConfForm, postUpdateGradingConfigCall, testDataFiles, helperFiles));

        if (adapter instanceof ConfigurableWithTokilibFormat) {
            Call updateGradingConfigCall = routes.ProgrammingProblemGradingController.editGradingConfigByTokilibFormat(problem.getId());
            content.appendLayout(c -> tokilibLayout.render(updateGradingConfigCall, c));
        } else if (adapter instanceof ConfigurableWithAutoPopulation) {
            Call updateGradingConfigCall = routes.ProgrammingProblemGradingController.editGradingConfigByAutoPopulation(problem.getId());
            content.appendLayout(c -> autoPopulationLayout.render(updateGradingConfigCall, c));
        }

        appendSubtabsLayout(content, problem);

        ProgrammingProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        SandalphonControllerUtils.getInstance().appendSidebarLayout(content);
        appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.programming.grading.config.update"), routes.ProgrammingProblemGradingController.editGradingConfig(problem.getId())));
        SandalphonControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Grading Config");

        return SandalphonControllerUtils.getInstance().lazyOk(content);
    }

    private Result showListGradingTestDataFiles(Form<UploadFileForm> uploadFileForm, Problem problem, List<FileInfo> testDataFiles) {
        LazyHtml content = new LazyHtml(listGradingTestDataFilesView.render(uploadFileForm, problem.getId(), testDataFiles));
        appendSubtabsLayout(content, problem);
        ProgrammingProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        SandalphonControllerUtils.getInstance().appendSidebarLayout(content);
        appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.programming.grading.testData.list"), routes.ProgrammingProblemGradingController.listGradingTestDataFiles(problem.getId())));
        SandalphonControllerUtils.getInstance().appendTemplateLayout(content, "Problem - List Grading Test Data Files");

        return SandalphonControllerUtils.getInstance().lazyOk(content);
    }

    private Result showListGradingHelperFiles(Form<UploadFileForm> uploadFileForm, Problem problem, List<FileInfo> helperFiles) {
        LazyHtml content = new LazyHtml(listGradingHelperFilesView.render(uploadFileForm, problem.getId(), helperFiles));
        appendSubtabsLayout(content, problem);
        ProgrammingProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        SandalphonControllerUtils.getInstance().appendSidebarLayout(content);
        appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.programming.grading.helper.list"), routes.ProgrammingProblemGradingController.listGradingHelperFiles(problem.getId())));
        SandalphonControllerUtils.getInstance().appendTemplateLayout(content, "Problem - List Grading Helper Files");

        return SandalphonControllerUtils.getInstance().lazyOk(content);
    }

    private Result showEditLanguageRestriction(Form<LanguageRestrictionEditForm> languageRestrictionEditForm, Problem problem) {
        LazyHtml content = new LazyHtml(editLanguageRestrictionView.render(languageRestrictionEditForm, problem));
        appendSubtabsLayout(content, problem);
        ProgrammingProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        SandalphonControllerUtils.getInstance().appendSidebarLayout(content);
        appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.programming.grading.languageRestriction.update"), routes.ProgrammingProblemGradingController.editLanguageRestriction(problem.getId())));
        SandalphonControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Language Restriction");

        return SandalphonControllerUtils.getInstance().lazyOk(content);
    }

    private void appendSubtabsLayout(LazyHtml content, Problem problem) {
        content.appendLayout(c -> subtabLayout.render(ImmutableList.of(
                new InternalLink(Messages.get("problem.programming.grading.engine"), routes.ProgrammingProblemGradingController.editGradingEngine(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.config"), routes.ProgrammingProblemGradingController.editGradingConfig(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.testData"), routes.ProgrammingProblemGradingController.listGradingTestDataFiles(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.helper"), routes.ProgrammingProblemGradingController.listGradingHelperFiles(problem.getId())),
                new InternalLink(Messages.get("problem.programming.grading.languageRestriction"), routes.ProgrammingProblemGradingController.editLanguageRestriction(problem.getId()))
        ), c));
    }

    private void appendBreadcrumbsLayout(LazyHtml content, Problem problem, InternalLink lastLink) {
        SandalphonControllerUtils.getInstance().appendBreadcrumbsLayout(content,
                ProblemControllerUtils.getProblemBreadcrumbsBuilder(problem)
                .add(new InternalLink(Messages.get("problem.programming.grading"), routes.ProgrammingProblemController.jumpToGrading(problem.getId())))
                .add(lastLink)
                .build()
        );
    }
}
