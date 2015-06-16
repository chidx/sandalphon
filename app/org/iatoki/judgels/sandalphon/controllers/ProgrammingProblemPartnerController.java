package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.JudgelsUtils;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.controllers.BaseController;
import org.iatoki.judgels.commons.views.html.layouts.heading3Layout;
import org.iatoki.judgels.jophiel.Jophiel;
import org.iatoki.judgels.jophiel.UserInfo;
import org.iatoki.judgels.sandalphon.JidCacheService;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemNotFoundException;
import org.iatoki.judgels.sandalphon.ProblemPartner;
import org.iatoki.judgels.sandalphon.ProblemPartnerConfig;
import org.iatoki.judgels.sandalphon.ProblemPartnerConfigBuilder;
import org.iatoki.judgels.sandalphon.ProblemPartnerNotFoundException;
import org.iatoki.judgels.sandalphon.ProblemService;
import org.iatoki.judgels.sandalphon.controllers.security.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.security.HasRole;
import org.iatoki.judgels.sandalphon.controllers.security.LoggedIn;
import org.iatoki.judgels.sandalphon.forms.ProblemPartnerUpsertForm;
import org.iatoki.judgels.sandalphon.forms.ProblemPartnerUsernameForm;
import org.iatoki.judgels.sandalphon.forms.programming.ProgrammingPartnerUpsertForm;
import org.iatoki.judgels.sandalphon.programming.ProgrammingProblemPartnerConfig;
import org.iatoki.judgels.sandalphon.programming.ProgrammingProblemPartnerConfigBuilder;
import org.iatoki.judgels.sandalphon.programming.ProgrammingProblemService;
import org.iatoki.judgels.sandalphon.views.html.programming.partner.addPartnerView;
import org.iatoki.judgels.sandalphon.views.html.programming.partner.updatePartnerView;
import play.data.Form;
import play.db.jpa.Transactional;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.RequireCSRFCheck;
import play.i18n.Messages;
import play.mvc.Http;
import play.mvc.Result;

import java.io.IOException;
import java.util.Set;

@Transactional
@Authenticated(value = {LoggedIn.class, HasRole.class})
public final class ProgrammingProblemPartnerController extends BaseController {
    private final Jophiel jophiel;
    private final ProblemService problemService;
    private final ProgrammingProblemService programmingProblemService;

    public ProgrammingProblemPartnerController(Jophiel jophiel, ProblemService problemService, ProgrammingProblemService programmingProblemService) {
        this.jophiel = jophiel;
        this.problemService = problemService;
        this.programmingProblemService = programmingProblemService;
    }

    @AddCSRFToken
    public Result addPartner(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (ProblemControllerUtils.isAuthorOrAbove(problem)) {
            Form<ProblemPartnerUsernameForm> usernameForm = Form.form(ProblemPartnerUsernameForm.class);
            Form<ProblemPartnerUpsertForm> problemForm = Form.form(ProblemPartnerUpsertForm.class);
            Form<ProgrammingPartnerUpsertForm> programmingForm = Form.form(ProgrammingPartnerUpsertForm.class);

            ControllerUtils.getInstance().addActivityLog("Try to add partner of problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return showAddPartner(usernameForm, problemForm, programmingForm, problem);
        } else {
            return notFound();
        }
    }

    @RequireCSRFCheck
    public Result postAddPartner(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (ProblemControllerUtils.isAuthorOrAbove(problem)) {
            Form<ProblemPartnerUsernameForm> usernameForm = Form.form(ProblemPartnerUsernameForm.class).bindFromRequest();
            Form<ProblemPartnerUpsertForm> problemForm = Form.form(ProblemPartnerUpsertForm.class).bindFromRequest();
            Form<ProgrammingPartnerUpsertForm> programmingForm = Form.form(ProgrammingPartnerUpsertForm.class).bindFromRequest();

            if (usernameForm.hasErrors() || usernameForm.hasGlobalErrors()) {
                return showAddPartner(usernameForm, problemForm, programmingForm, problem);
            }

            if (problemForm.hasErrors() || problemForm.hasGlobalErrors()) {
                return showAddPartner(usernameForm, problemForm, programmingForm, problem);
            }

            if (programmingForm.hasErrors() || programmingForm.hasGlobalErrors()) {
                return showAddPartner(usernameForm, problemForm, programmingForm, problem);
            }

            String username = usernameForm.get().username;
            ProblemPartnerUpsertForm problemData = problemForm.get();
            ProgrammingPartnerUpsertForm programmingData = programmingForm.get();

            String userJid = jophiel.verifyUsername(username);
            if (userJid == null) {
                usernameForm.reject("username", Messages.get("problem.partner.usernameNotFound"));
                return showAddPartner(usernameForm, problemForm, programmingForm, problem);
            }

            try {
                UserInfo user = jophiel.getUserByUserJid(userJid);
                JidCacheService.getInstance().putDisplayName(user.getJid(), JudgelsUtils.getUserDisplayName(user.getUsername(), user.getName()), IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

                if (problemService.isProblemPartnerByUserJid(problem.getJid(), userJid)) {
                    usernameForm.reject("username", Messages.get("problem.partner.already"));
                    return showAddPartner(usernameForm, problemForm, programmingForm, problem);
                }

                ProblemPartnerConfig problemConfig = new ProblemPartnerConfigBuilder()
                      .setIsAllowedToUpdateProblem(problemData.isAllowedToUpdateProblem)
                      .setIsAllowedToUpdateStatement(problemData.isAllowedToUpdateStatement)
                      .setIsAllowedToUploadStatementResources(problemData.isAllowedToUploadStatementResources)
                      .setAllowedStatementLanguagesToView(splitByComma(problemData.allowedStatementLanguagesToView))
                      .setAllowedStatementLanguagesToUpdate(splitByComma(problemData.allowedStatementLanguagesToUpdate))
                      .setIsAllowedToManageStatementLanguages(problemData.isAllowedToManageStatementLanguages)
                      .setIsAllowedToViewVersionHistory(problemData.isAllowedToViewVersionHistory)
                      .setIsAllowedToRestoreVersionHistory(problemData.isAllowedToRestoreVersionHistory)
                      .setIsAllowedToManageProblemClients(problemData.isAllowedToManageProblemClients)
                      .build();

                ProgrammingProblemPartnerConfig programmingConfig = new ProgrammingProblemPartnerConfigBuilder()
                      .setIsAllowedToSubmit(programmingData.isAllowedToSubmit)
                      .setIsAllowedToManageGrading(programmingData.isAllowedToManageGrading)
                      .build();

                problemService.createProblemPartner(problem.getId(), userJid, problemConfig, programmingConfig);

                ControllerUtils.getInstance().addActivityLog("Add partner " + userJid + " of problem " + problem.getName() + ".");

                return redirect(routes.ProblemPartnerController.viewPartners(problem.getId()));
            } catch (IOException e) {
                return notFound();
            }
        } else {
            return notFound();
        }
    }

    @AddCSRFToken
    public Result updatePartner(long problemId, long partnerId) throws ProblemNotFoundException, ProblemPartnerNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (ProblemControllerUtils.isAuthorOrAbove(problem)) {
            ProblemPartner problemPartner = problemService.findProblemPartnerByProblemPartnerId(partnerId);

            ProblemPartnerConfig problemConfig = problemPartner.getBaseConfig();
            ProblemPartnerUpsertForm problemData = new ProblemPartnerUpsertForm();

            problemData.isAllowedToUpdateProblem = problemConfig.isAllowedToUpdateProblem();
            problemData.isAllowedToUpdateStatement = problemConfig.isAllowedToUpdateStatement();
            problemData.isAllowedToUploadStatementResources = problemConfig.isAllowedToUploadStatementResources();
            problemData.allowedStatementLanguagesToView = combineByComma(problemConfig.getAllowedStatementLanguagesToView());
            problemData.allowedStatementLanguagesToUpdate = combineByComma(problemConfig.getAllowedStatementLanguagesToUpdate());
            problemData.isAllowedToManageStatementLanguages = problemConfig.isAllowedToManageStatementLanguages();
            problemData.isAllowedToViewVersionHistory = problemConfig.isAllowedToViewVersionHistory();
            problemData.isAllowedToRestoreVersionHistory = problemConfig.isAllowedToRestoreVersionHistory();
            problemData.isAllowedToManageProblemClients = problemConfig.isAllowedToManageProblemClients();

            Form<ProblemPartnerUpsertForm> problemForm = Form.form(ProblemPartnerUpsertForm.class).fill(problemData);

            ProgrammingProblemPartnerConfig programmingConfig = problemPartner.getChildConfig(ProgrammingProblemPartnerConfig.class);
            ProgrammingPartnerUpsertForm programmingData = new ProgrammingPartnerUpsertForm();

            programmingData.isAllowedToSubmit = programmingConfig.isAllowedToSubmit();
            programmingData.isAllowedToManageGrading = programmingConfig.isAllowedToManageGrading();

            Form<ProgrammingPartnerUpsertForm> programmingForm = Form.form(ProgrammingPartnerUpsertForm.class).fill(programmingData);

            ControllerUtils.getInstance().addActivityLog("Try to update partner " + problemPartner.getPartnerJid() + " of problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return showUpdatePartner(problemForm, programmingForm, problem, problemPartner);
        } else {
            return notFound();
        }
    }

    @RequireCSRFCheck
    public Result postUpdatePartner(long problemId, long partnerId) throws ProblemNotFoundException, ProblemPartnerNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (ProblemControllerUtils.isAuthorOrAbove(problem)) {
            ProblemPartner problemPartner = problemService.findProblemPartnerByProblemPartnerId(partnerId);

            Form<ProblemPartnerUpsertForm> problemForm = Form.form(ProblemPartnerUpsertForm.class).bindFromRequest();
            Form<ProgrammingPartnerUpsertForm> programmingForm = Form.form(ProgrammingPartnerUpsertForm.class).bindFromRequest();

            if (problemForm.hasErrors() || problemForm.hasGlobalErrors()) {
                return showUpdatePartner(problemForm, programmingForm, problem, problemPartner);
            }

            ProblemPartnerUpsertForm problemData = problemForm.get();

            ProblemPartnerConfig problemConfig = new ProblemPartnerConfigBuilder()
                    .setIsAllowedToUpdateProblem(problemData.isAllowedToUpdateProblem)
                    .setIsAllowedToUpdateStatement(problemData.isAllowedToUpdateStatement)
                    .setIsAllowedToUploadStatementResources(problemData.isAllowedToUploadStatementResources)
                    .setAllowedStatementLanguagesToView(splitByComma(problemData.allowedStatementLanguagesToView))
                    .setAllowedStatementLanguagesToUpdate(splitByComma(problemData.allowedStatementLanguagesToUpdate))
                    .setIsAllowedToManageStatementLanguages(problemData.isAllowedToManageStatementLanguages)
                    .setIsAllowedToViewVersionHistory(problemData.isAllowedToViewVersionHistory)
                    .setIsAllowedToRestoreVersionHistory(problemData.isAllowedToRestoreVersionHistory)
                    .setIsAllowedToManageProblemClients(problemData.isAllowedToManageProblemClients)
                    .build();

            ProgrammingPartnerUpsertForm programmingData = programmingForm.get();

            ProgrammingProblemPartnerConfig programmingConfig = new ProgrammingProblemPartnerConfigBuilder()
                    .setIsAllowedToSubmit(programmingData.isAllowedToSubmit)
                    .setIsAllowedToManageGrading(programmingData.isAllowedToManageGrading)
                    .build();


            problemService.updateProblemPartner(partnerId, problemConfig, programmingConfig);

            ControllerUtils.getInstance().addActivityLog("Update partner " + problemPartner.getPartnerJid() + " of problem " + problem.getName() + ".");

            return redirect(routes.ProblemPartnerController.updatePartner(problem.getId(), problemPartner.getId()));
        } else {
            return notFound();
        }
    }

    private Result showAddPartner(Form<ProblemPartnerUsernameForm> usernameForm, Form<ProblemPartnerUpsertForm> problemForm, Form<ProgrammingPartnerUpsertForm> programmingForm, Problem problem) {
        LazyHtml content = new LazyHtml(addPartnerView.render(usernameForm, problemForm, programmingForm, problem, jophiel.getAutoCompleteEndPoint()));

        content.appendLayout(c -> heading3Layout.render(Messages.get("problem.partner.add"), c));
        ProgrammingProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ProblemPartnerControllerUtils.appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.partner.add"), routes.ProgrammingProblemPartnerController.addPartner(problem.getId())));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Add Partner");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Result showUpdatePartner(Form<ProblemPartnerUpsertForm> problemForm, Form<ProgrammingPartnerUpsertForm> programmingForm, Problem problem, ProblemPartner problemPartner) {
        LazyHtml content = new LazyHtml(updatePartnerView.render(problemForm, programmingForm, problem, problemPartner));

        content.appendLayout(c -> heading3Layout.render(Messages.get("problem.partner.update") + ": " + JidCacheService.getInstance().getDisplayName(problemPartner.getPartnerJid()), c));
        ProgrammingProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ProblemPartnerControllerUtils.appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.partner.update"), routes.ProgrammingProblemPartnerController.updatePartner(problem.getId(), problemPartner.getId())));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Partner");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private Set<String> splitByComma(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        return Sets.newHashSet(s.split(","));
    }

    private String combineByComma(Set<String> list) {
        if (list == null) {
            return null;
        }
        return Joiner.on(",").join(list);
    }
}
