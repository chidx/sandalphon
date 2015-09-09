package org.iatoki.judgels.sandalphon.controllers;

import org.iatoki.judgels.jophiel.Jophiel;
import org.iatoki.judgels.jophiel.PublicUser;
import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.InternalLink;
import org.iatoki.judgels.play.JudgelsPlayUtils;
import org.iatoki.judgels.play.LazyHtml;
import org.iatoki.judgels.play.controllers.AbstractJudgelsController;
import org.iatoki.judgels.play.views.html.layouts.heading3Layout;
import org.iatoki.judgels.sandalphon.BundleProblemPartnerConfig;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemNotFoundException;
import org.iatoki.judgels.sandalphon.ProblemPartner;
import org.iatoki.judgels.sandalphon.ProblemPartnerConfig;
import org.iatoki.judgels.sandalphon.ProblemPartnerConfigBuilder;
import org.iatoki.judgels.sandalphon.ProblemPartnerNotFoundException;
import org.iatoki.judgels.sandalphon.controllers.securities.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.securities.HasRole;
import org.iatoki.judgels.sandalphon.controllers.securities.LoggedIn;
import org.iatoki.judgels.sandalphon.forms.BundlePartnerUpsertForm;
import org.iatoki.judgels.sandalphon.forms.ProblemPartnerUpsertForm;
import org.iatoki.judgels.sandalphon.forms.ProblemPartnerUsernameForm;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import org.iatoki.judgels.sandalphon.services.impls.JidCacheServiceImpl;
import org.iatoki.judgels.sandalphon.views.html.problem.bundle.partner.addPartnerView;
import org.iatoki.judgels.sandalphon.views.html.problem.bundle.partner.updatePartnerView;
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
import java.io.IOException;

@Authenticated(value = {LoggedIn.class, HasRole.class})
@Singleton
@Named
public final class BundleProblemPartnerController extends AbstractJudgelsController {

    private final Jophiel jophiel;
    private final ProblemService problemService;

    @Inject
    public BundleProblemPartnerController(Jophiel jophiel, ProblemService problemService) {
        this.jophiel = jophiel;
        this.problemService = problemService;
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result addPartner(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProblemControllerUtils.isAuthorOrAbove(problem)) {
            return notFound();
        }

        Form<ProblemPartnerUsernameForm> usernameForm = Form.form(ProblemPartnerUsernameForm.class);
        Form<ProblemPartnerUpsertForm> problemForm = Form.form(ProblemPartnerUpsertForm.class);
        Form<BundlePartnerUpsertForm> bundleForm = Form.form(BundlePartnerUpsertForm.class);

        SandalphonControllerUtils.getInstance().addActivityLog("Try to add partner of problem " + problem.getSlug() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return showAddPartner(usernameForm, problemForm, bundleForm, problem);
    }

    @Transactional
    @RequireCSRFCheck
    public Result postAddPartner(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProblemControllerUtils.isAuthorOrAbove(problem)) {
            return notFound();
        }

        Form<ProblemPartnerUsernameForm> usernameForm = Form.form(ProblemPartnerUsernameForm.class).bindFromRequest();
        Form<ProblemPartnerUpsertForm> problemForm = Form.form(ProblemPartnerUpsertForm.class).bindFromRequest();
        Form<BundlePartnerUpsertForm> bundleForm = Form.form(BundlePartnerUpsertForm.class).bindFromRequest();

        if (formHasErrors(usernameForm) || formHasErrors(problemForm) || formHasErrors(bundleForm)) {
            return showAddPartner(usernameForm, problemForm, bundleForm, problem);
        }

        String username = usernameForm.get().username;
        ProblemPartnerUpsertForm problemData = problemForm.get();
        BundlePartnerUpsertForm bundleData = bundleForm.get();

        String userJid;
        try {
            userJid = jophiel.verifyUsername(username);
        } catch (IOException e) {
            return notFound();
        }

        if (userJid == null) {
            usernameForm.reject("username", Messages.get("problem.partner.usernameNotFound"));
            return showAddPartner(usernameForm, problemForm, bundleForm, problem);
        }

        PublicUser publicUser;
        try {
            publicUser = jophiel.getPublicUserByJid(userJid);
        } catch (IOException e) {
            return notFound();
        }

        JidCacheServiceImpl.getInstance().putDisplayName(publicUser.getJid(), JudgelsPlayUtils.getUserDisplayName(publicUser.getUsername()), IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

        if (problemService.isUserPartnerForProblem(problem.getJid(), userJid)) {
            usernameForm.reject("username", Messages.get("problem.partner.already"));
            return showAddPartner(usernameForm, problemForm, bundleForm, problem);
        }

        ProblemPartnerConfig problemConfig = new ProblemPartnerConfigBuilder()
              .setIsAllowedToUpdateProblem(problemData.isAllowedToUpdateProblem)
              .setIsAllowedToUpdateStatement(problemData.isAllowedToUpdateStatement)
              .setIsAllowedToUploadStatementResources(problemData.isAllowedToUploadStatementResources)
              .setAllowedStatementLanguagesToView(PartnerControllerUtils.splitByComma(problemData.allowedStatementLanguagesToView))
              .setAllowedStatementLanguagesToUpdate(PartnerControllerUtils.splitByComma(problemData.allowedStatementLanguagesToUpdate))
              .setIsAllowedToManageStatementLanguages(problemData.isAllowedToManageStatementLanguages)
              .setIsAllowedToViewVersionHistory(problemData.isAllowedToViewVersionHistory)
              .setIsAllowedToRestoreVersionHistory(problemData.isAllowedToRestoreVersionHistory)
              .setIsAllowedToManageProblemClients(problemData.isAllowedToManageProblemClients)
              .build();

        BundleProblemPartnerConfig bundleConfig = new BundleProblemPartnerConfig(bundleData.isAllowedToSubmit, bundleData.isAllowedToManageItems);

        problemService.createProblemPartner(problem.getJid(), userJid, problemConfig, bundleConfig, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

        SandalphonControllerUtils.getInstance().addActivityLog("Add partner " + userJid + " of problem " + problem.getSlug() + ".");

        return redirect(routes.ProblemPartnerController.viewPartners(problem.getId()));
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result updatePartner(long problemId, long partnerId) throws ProblemNotFoundException, ProblemPartnerNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProblemControllerUtils.isAuthorOrAbove(problem)) {
            return notFound();
        }

        ProblemPartner problemPartner = problemService.findProblemPartnerById(partnerId);

        ProblemPartnerConfig problemConfig = problemPartner.getBaseConfig();
        ProblemPartnerUpsertForm problemData = new ProblemPartnerUpsertForm();

        problemData.isAllowedToUpdateProblem = problemConfig.isAllowedToUpdateProblem();
        problemData.isAllowedToUpdateStatement = problemConfig.isAllowedToUpdateStatement();
        problemData.isAllowedToUploadStatementResources = problemConfig.isAllowedToUploadStatementResources();
        problemData.allowedStatementLanguagesToView = PartnerControllerUtils.combineByComma(problemConfig.getAllowedStatementLanguagesToView());
        problemData.allowedStatementLanguagesToUpdate = PartnerControllerUtils.combineByComma(problemConfig.getAllowedStatementLanguagesToUpdate());
        problemData.isAllowedToManageStatementLanguages = problemConfig.isAllowedToManageStatementLanguages();
        problemData.isAllowedToViewVersionHistory = problemConfig.isAllowedToViewVersionHistory();
        problemData.isAllowedToRestoreVersionHistory = problemConfig.isAllowedToRestoreVersionHistory();
        problemData.isAllowedToManageProblemClients = problemConfig.isAllowedToManageProblemClients();

        Form<ProblemPartnerUpsertForm> problemForm = Form.form(ProblemPartnerUpsertForm.class).fill(problemData);

        BundleProblemPartnerConfig bundleConfig = problemPartner.getChildConfig(BundleProblemPartnerConfig.class);
        BundlePartnerUpsertForm bundleData = new BundlePartnerUpsertForm();

        bundleData.isAllowedToManageItems = bundleConfig.isAllowedToManageItems();

        Form<BundlePartnerUpsertForm> bundleForm = Form.form(BundlePartnerUpsertForm.class).fill(bundleData);

        SandalphonControllerUtils.getInstance().addActivityLog("Try to update partner " + problemPartner.getPartnerJid() + " of problem " + problem.getSlug() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return showUpdatePartner(problemForm, bundleForm, problem, problemPartner);
    }

    @Transactional
    @RequireCSRFCheck
    public Result postUpdatePartner(long problemId, long partnerId) throws ProblemNotFoundException, ProblemPartnerNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (!ProblemControllerUtils.isAuthorOrAbove(problem)) {
            return notFound();
        }

        ProblemPartner problemPartner = problemService.findProblemPartnerById(partnerId);

        Form<ProblemPartnerUpsertForm> problemForm = Form.form(ProblemPartnerUpsertForm.class).bindFromRequest();
        Form<BundlePartnerUpsertForm> bundleForm = Form.form(BundlePartnerUpsertForm.class).bindFromRequest();

        if (formHasErrors(problemForm) || formHasErrors(bundleForm)) {
            return showUpdatePartner(problemForm, bundleForm, problem, problemPartner);
        }

        ProblemPartnerUpsertForm problemData = problemForm.get();

        ProblemPartnerConfig problemConfig = new ProblemPartnerConfigBuilder()
              .setIsAllowedToUpdateProblem(problemData.isAllowedToUpdateProblem)
              .setIsAllowedToUpdateStatement(problemData.isAllowedToUpdateStatement)
              .setIsAllowedToUploadStatementResources(problemData.isAllowedToUploadStatementResources)
              .setAllowedStatementLanguagesToView(PartnerControllerUtils.splitByComma(problemData.allowedStatementLanguagesToView))
              .setAllowedStatementLanguagesToUpdate(PartnerControllerUtils.splitByComma(problemData.allowedStatementLanguagesToUpdate))
              .setIsAllowedToManageStatementLanguages(problemData.isAllowedToManageStatementLanguages)
              .setIsAllowedToViewVersionHistory(problemData.isAllowedToViewVersionHistory)
              .setIsAllowedToRestoreVersionHistory(problemData.isAllowedToRestoreVersionHistory)
              .setIsAllowedToManageProblemClients(problemData.isAllowedToManageProblemClients)
              .build();

        BundlePartnerUpsertForm bundleData = bundleForm.get();

        BundleProblemPartnerConfig bundleConfig = new BundleProblemPartnerConfig(bundleData.isAllowedToSubmit, bundleData.isAllowedToManageItems);

        problemService.updateProblemPartner(partnerId, problemConfig, bundleConfig, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

        SandalphonControllerUtils.getInstance().addActivityLog("Update partner " + problemPartner.getPartnerJid() + " of problem " + problem.getSlug() + ".");

        return redirect(routes.ProblemPartnerController.updatePartner(problem.getId(), problemPartner.getId()));
    }

    private Result showAddPartner(Form<ProblemPartnerUsernameForm> usernameForm, Form<ProblemPartnerUpsertForm> problemForm, Form<BundlePartnerUpsertForm> bundleForm, Problem problem) {
        LazyHtml content = new LazyHtml(addPartnerView.render(usernameForm, problemForm, bundleForm, problem, jophiel.getAutoCompleteEndPoint()));

        content.appendLayout(c -> heading3Layout.render(Messages.get("problem.partner.add"), c));
        BundleProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        SandalphonControllerUtils.getInstance().appendSidebarLayout(content);
        ProblemPartnerControllerUtils.appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.partner.add"), routes.BundleProblemPartnerController.addPartner(problem.getId())));
        SandalphonControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Add Partner");

        return SandalphonControllerUtils.getInstance().lazyOk(content);
    }

    private Result showUpdatePartner(Form<ProblemPartnerUpsertForm> problemForm, Form<BundlePartnerUpsertForm> bundleForm, Problem problem, ProblemPartner problemPartner) {
        LazyHtml content = new LazyHtml(updatePartnerView.render(problemForm, bundleForm, problem, problemPartner));

        content.appendLayout(c -> heading3Layout.render(Messages.get("problem.partner.update") + ": " + JidCacheServiceImpl.getInstance().getDisplayName(problemPartner.getPartnerJid()), c));
        BundleProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        SandalphonControllerUtils.getInstance().appendSidebarLayout(content);
        ProblemPartnerControllerUtils.appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.partner.update"), routes.BundleProblemPartnerController.updatePartner(problem.getId(), problemPartner.getId())));
        SandalphonControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Partner");

        return SandalphonControllerUtils.getInstance().lazyOk(content);
    }
}
