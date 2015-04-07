package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.iatoki.judgels.commons.FileInfo;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemService;
import org.iatoki.judgels.sandalphon.ProblemType;
import org.iatoki.judgels.sandalphon.StatementLanguageStatus;
import org.iatoki.judgels.sandalphon.controllers.security.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.security.HasRole;
import org.iatoki.judgels.sandalphon.controllers.security.LoggedIn;
import org.iatoki.judgels.sandalphon.forms.UpdateStatementForm;
import org.iatoki.judgels.sandalphon.forms.UploadFileForm;
import org.iatoki.judgels.sandalphon.views.html.problem.statement.listStatementLanguagesView;
import org.iatoki.judgels.sandalphon.views.html.problem.statement.listStatementMediaFilesView;
import org.iatoki.judgels.sandalphon.views.html.problem.statement.updateStatementView;
import play.data.DynamicForm;
import play.data.Form;
import play.db.jpa.Transactional;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.RequireCSRFCheck;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Transactional
@Authenticated(value = {LoggedIn.class, HasRole.class})
public class ProblemStatementController extends Controller {
    private final ProblemService problemService;

    public ProblemStatementController(ProblemService problemService) {
        this.problemService = problemService;
    }

    public Result viewStatement(long problemId) {
        Problem problem = problemService.findProblemById(problemId);

        if (problem.getType() == ProblemType.PROGRAMMING) {
            return redirect(routes.ProgrammingProblemStatementController.viewStatement(problem.getId()));
        } else {
            return badRequest();
        }
    }

    public Result viewStatementSwitchLanguage(long problemId) {
        String languageCode = DynamicForm.form().bindFromRequest().get("langCode");
        ProblemControllerUtils.setCurrentStatementLanguage(languageCode);

        ControllerUtils.getInstance().addActivityLog("Switch view statement to " + languageCode + " of problem " + problemId + " <a href=\"\" + \"http://\" + Http.Context.current().request().host() + Http.Context.current().request().uri() + \"\">link</a>.");

        return redirect(routes.ProblemStatementController.viewStatement(problemId));
    }

    public Result updateStatementSwitchLanguage(long problemId) {
        String languageCode = DynamicForm.form().bindFromRequest().get("langCode");
        ProblemControllerUtils.setCurrentStatementLanguage(languageCode);

        ControllerUtils.getInstance().addActivityLog("Switch update statement to " + languageCode + " of problem " + problemId + " <a href=\"\" + \"http://\" + Http.Context.current().request().host() + Http.Context.current().request().uri() + \"\">link</a>.");

        return redirect(routes.ProblemStatementController.updateStatement(problemId));
    }

    @AddCSRFToken
    public Result updateStatement(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        ProblemControllerUtils.establishStatementLanguage(problemService, problem);

        if (ProblemControllerUtils.isAllowedToUpdateStatementInLanguage(problemService, problem)) {
            String statement = problemService.getStatement(IdentityUtils.getUserJid(), problem.getJid(), ProblemControllerUtils.getCurrentStatementLanguage());

            Form<UpdateStatementForm> form = Form.form(UpdateStatementForm.class);
            form = form.bind(ImmutableMap.of("statement", statement));

            Set<String> allowedLanguages = ProblemControllerUtils.getAllowedLanguagesToUpdate(problemService, problem);

            ControllerUtils.getInstance().addActivityLog("Try to update statement of problem " + problem.getName() + " <a href=\"\" + \"http://\" + Http.Context.current().request().host() + Http.Context.current().request().uri() + \"\">link</a>.");

            return showUpdateStatement(form, problem, allowedLanguages);
        } else {
            return notFound();
        }
    }

    @RequireCSRFCheck
    public Result postUpdateStatement(long problemId) {
        Problem problem = problemService.findProblemById(problemId);
        ProblemControllerUtils.establishStatementLanguage(problemService, problem);

        if (ProblemControllerUtils.isAllowedToUpdateStatementInLanguage(problemService, problem)) {
            Form<UpdateStatementForm> form = Form.form(UpdateStatementForm.class).bindFromRequest();
            if (form.hasErrors() || form.hasGlobalErrors()) {
                Set<String> allowedLanguages = ProblemControllerUtils.getAllowedLanguagesToUpdate(problemService, problem);
                return showUpdateStatement(form, problem, allowedLanguages);
            } else {
                problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

                problemService.updateStatement(IdentityUtils.getUserJid(), problemId, ProblemControllerUtils.getCurrentStatementLanguage(), form.get().statement);

                ControllerUtils.getInstance().addActivityLog("Update statement of problem " + problem.getName() + ".");

                return redirect(routes.ProblemStatementController.updateStatement(problem.getId()));
            }
        } else {
            return notFound();
        }
    }


    @AddCSRFToken
    public Result listStatementMediaFiles(long problemId) {
        Problem problem = problemService.findProblemById(problemId);

        Form<UploadFileForm> form = Form.form(UploadFileForm.class);
        
        boolean isAllowedToUploadMediaFiles = ProblemControllerUtils.isAllowedToUploadStatementResources(problemService, problem);
        
        List<FileInfo> mediaFiles = problemService.getStatementMediaFiles(IdentityUtils.getUserJid(), problem.getJid());

        ControllerUtils.getInstance().addActivityLog("List statement media files of problem " + problem.getName() + " <a href=\"\" + \"http://\" + Http.Context.current().request().host() + Http.Context.current().request().uri() + \"\">link</a>.");

        return showListStatementMediaFiles(form, problem, mediaFiles, isAllowedToUploadMediaFiles);
    }

    @RequireCSRFCheck
    public Result postUploadStatementMediaFiles(long problemId) {
        Problem problem = problemService.findProblemById(problemId);

        if (ProblemControllerUtils.isAllowedToUploadStatementResources(problemService, problem)) {
            Http.MultipartFormData body = request().body().asMultipartFormData();
            Http.MultipartFormData.FilePart file;

            file = body.getFile("file");
            if (file != null) {
                File mediaFile = file.getFile();
                problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

                problemService.uploadStatementMediaFile(IdentityUtils.getUserJid(), problem.getId(), mediaFile, file.getFilename());

                ControllerUtils.getInstance().addActivityLog("Upload statement media file of problem " + problem.getName() + " <a href=\"\" + \"http://\" + Http.Context.current().request().host() + Http.Context.current().request().uri() + \"\">link</a>.");

                return redirect(routes.ProblemStatementController.listStatementMediaFiles(problem.getId()));
            }

            file = body.getFile("fileZipped");
            if (file != null) {
                File mediaFile = file.getFile();
                problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

                problemService.uploadStatementMediaFileZipped(IdentityUtils.getUserJid(), problem.getId(), mediaFile);

                ControllerUtils.getInstance().addActivityLog("Upload statement zipped media files of problem " + problem.getName() + " <a href=\"\" + \"http://\" + Http.Context.current().request().host() + Http.Context.current().request().uri() + \"\">link</a>.");

                return redirect(routes.ProblemStatementController.listStatementMediaFiles(problem.getId()));
            }

            return redirect(routes.ProblemStatementController.listStatementMediaFiles(problem.getId()));
        } else {
            return notFound();
        }
    }

    public Result downloadStatementMediaFile(long id, String filename) {
        Problem problem = problemService.findProblemById(id);
        String mediaURL = problemService.getStatementMediaFileURL(IdentityUtils.getUserJid(), problem.getJid(), filename);

        ControllerUtils.getInstance().addActivityLog("Download media file " + filename + " of problem " + problem.getName() + " <a href=\"\" + \"http://\" + Http.Context.current().request().host() + Http.Context.current().request().uri() + \"\">link</a>.");

        try {
            new URL(mediaURL);
            return redirect(mediaURL);
        } catch (MalformedURLException e) {
            File mediaFile = new File(mediaURL);
            return ProblemControllerUtils.downloadFile(mediaFile);
        }
    }

    public Result listStatementLanguages(long problemId) {
        Problem problem = problemService.findProblemById(problemId);

        if (ProblemControllerUtils.isAllowedToManageStatementLanguages(problemService, problem)) {
            Map<String, StatementLanguageStatus> availableLanguages = problemService.getAvailableLanguages(IdentityUtils.getUserJid(), problem.getJid());
            String defaultLanguage = problemService.getDefaultLanguage(IdentityUtils.getUserJid(), problem.getJid());

            LazyHtml content = new LazyHtml(listStatementLanguagesView.render(availableLanguages, defaultLanguage, problem.getId()));
            ProblemStatementControllerUtils.appendSubtabsLayout(content, problemService, problem);
            ProblemControllerUtils.appendTabsLayout(content, problemService, problem);
            ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
            ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
            ControllerUtils.getInstance().appendSidebarLayout(content);
            ProblemStatementControllerUtils.appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.statement.language.list"), routes.ProblemStatementController.listStatementLanguages(problem.getId())));
            ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Statement Languages");

            ControllerUtils.getInstance().addActivityLog("List statement languages of problem " + problem.getName() + " <a href=\"\" + \"http://\" + Http.Context.current().request().host() + Http.Context.current().request().uri() + \"\">link</a>.");

            return ControllerUtils.getInstance().lazyOk(content);
        } return notFound();
    }


    public Result postAddStatementLanguage(long problemId) {
        Problem problem = problemService.findProblemById(problemId);

        if (ProblemControllerUtils.isAllowedToManageStatementLanguages(problemService, problem)) {
            problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

            String languageCode = DynamicForm.form().bindFromRequest().get("langCode");
            problemService.addLanguage(IdentityUtils.getUserJid(), problem.getJid(), languageCode);

            ControllerUtils.getInstance().addActivityLog("Add statement language " + languageCode + " of problem " + problem.getName() + ".");

            return redirect(routes.ProblemStatementController.listStatementLanguages(problem.getId()));
        } else {
            return notFound();
        }
    }

    public Result enableStatementLanguage(long problemId, String languageCode) {
        Problem problem = problemService.findProblemById(problemId);

        if (ProblemControllerUtils.isAllowedToManageStatementLanguages(problemService, problem)) {
            problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

            problemService.enableLanguage(IdentityUtils.getUserJid(), problem.getJid(), languageCode);

            ControllerUtils.getInstance().addActivityLog("Enable statement language " + languageCode + " of problem " + problem.getName() + " <a href=\"\" + \"http://\" + Http.Context.current().request().host() + Http.Context.current().request().uri() + \"\">link</a>.");

            return redirect(routes.ProblemStatementController.listStatementLanguages(problem.getId()));
        } else {
            return notFound();
        }
    }


    public Result disableStatementLanguage(long problemId, String languageCode) {
        Problem problem = problemService.findProblemById(problemId);

        if (ProblemControllerUtils.isAllowedToManageStatementLanguages(problemService, problem)) {
            problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

            problemService.disableLanguage(IdentityUtils.getUserJid(), problem.getJid(), languageCode);

            if (ProblemControllerUtils.getCurrentStatementLanguage().equals(languageCode)) {
                ProblemControllerUtils.setCurrentStatementLanguage(problemService.getDefaultLanguage(IdentityUtils.getUserJid(), problem.getJid()));
            }

            ControllerUtils.getInstance().addActivityLog("Disable statement language " + languageCode + " of problem " + problem.getName() + " <a href=\"\" + \"http://\" + Http.Context.current().request().host() + Http.Context.current().request().uri() + \"\">link</a>.");

            return redirect(routes.ProblemStatementController.listStatementLanguages(problem.getId()));
        } else {
            return notFound();
        }
    }

    public Result makeDefaultStatementLanguage(long problemId, String languageCode) {
        Problem problem = problemService.findProblemById(problemId);

        if (ProblemControllerUtils.isAllowedToManageStatementLanguages(problemService, problem)) {
            problemService.createUserCloneIfNotExists(IdentityUtils.getUserJid(), problem.getJid());

            problemService.makeDefaultLanguage(IdentityUtils.getUserJid(), problem.getJid(), languageCode);

            ControllerUtils.getInstance().addActivityLog("Make statement language " + languageCode + " default of problem " + problem.getName() + " <a href=\"\" + \"http://\" + Http.Context.current().request().host() + Http.Context.current().request().uri() + \"\">link</a>.");

            return redirect(routes.ProblemStatementController.listStatementLanguages(problem.getId()));
        } else {
            return notFound();
        }
    }

    private Result showUpdateStatement(Form<UpdateStatementForm> form, Problem problem, Set<String> allowedLanguages) {
        LazyHtml content = new LazyHtml(updateStatementView.render(form, problem.getId()));
        ProblemControllerUtils.appendStatementLanguageSelectionLayout(content, ProblemControllerUtils.getCurrentStatementLanguage(), allowedLanguages, routes.ProblemStatementController.updateStatementSwitchLanguage(problem.getId()));
        ProblemStatementControllerUtils.appendSubtabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ProblemStatementControllerUtils.appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.statement.update"), routes.ProblemStatementController.updateStatement(problem.getId())));

        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Statement");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Result showListStatementMediaFiles(Form<UploadFileForm> form, Problem problem, List<FileInfo> mediaFiles, boolean isAllowedToUploadMediaFiles) {
        LazyHtml content = new LazyHtml(listStatementMediaFilesView.render(form, problem.getId(), mediaFiles, isAllowedToUploadMediaFiles));
        ProblemStatementControllerUtils.appendSubtabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ProblemStatementControllerUtils.appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.statement.media.list"), routes.ProblemStatementController.listStatementMediaFiles(problem.getId())));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Statement - List Media");

        return ControllerUtils.getInstance().lazyOk(content);
    }
}
