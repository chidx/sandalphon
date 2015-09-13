package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.InternalLink;
import org.iatoki.judgels.play.LazyHtml;
import org.iatoki.judgels.play.controllers.AbstractJudgelsController;
import org.iatoki.judgels.gabriel.GradingConfig;
import org.iatoki.judgels.gabriel.GradingEngineRegistry;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemNotFoundException;
import org.iatoki.judgels.sandalphon.ProblemStatement;
import org.iatoki.judgels.sandalphon.ProblemStatementUtils;
import org.iatoki.judgels.sandalphon.adapters.GradingEngineAdapterRegistry;
import org.iatoki.judgels.sandalphon.controllers.securities.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.securities.HasRole;
import org.iatoki.judgels.sandalphon.controllers.securities.LoggedIn;
import org.iatoki.judgels.sandalphon.LanguageRestriction;
import org.iatoki.judgels.sandalphon.LanguageRestrictionAdapter;
import org.iatoki.judgels.sandalphon.ProgrammingProblemStatementUtils;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import org.iatoki.judgels.sandalphon.services.ProgrammingProblemService;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Set;

@Authenticated(value = {LoggedIn.class, HasRole.class})
@Singleton
@Named
public final class ProgrammingProblemStatementController extends AbstractJudgelsController {

    private final ProblemService problemService;
    private final ProgrammingProblemService programmingProblemService;

    @Inject
    public ProgrammingProblemStatementController(ProblemService problemService, ProgrammingProblemService programmingProblemService) {
        this.problemService = problemService;
        this.programmingProblemService = programmingProblemService;
    }

    @Transactional(readOnly = true)
    public Result viewStatement(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);
        try {
            ProblemControllerUtils.establishStatementLanguage(problemService, problem);
        } catch (IOException e) {
            return notFound();
        }

        if (!ProblemControllerUtils.isAllowedToViewStatement(problemService, problem)) {
            return notFound();
        }

        ProblemStatement statement;
        try {
            statement = problemService.getStatement(IdentityUtils.getUserJid(), problem.getJid(), ProblemControllerUtils.getCurrentStatementLanguage());
        } catch (IOException e) {
            statement = new ProblemStatement(ProblemStatementUtils.getDefaultTitle(ProblemControllerUtils.getCurrentStatementLanguage()), ProgrammingProblemStatementUtils.getDefaultText(ProblemControllerUtils.getCurrentStatementLanguage()));
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
        LanguageRestriction languageRestriction;
        try {
            languageRestriction = programmingProblemService.getLanguageRestriction(IdentityUtils.getUserJid(), problem.getJid());
        } catch (IOException e) {
            languageRestriction = LanguageRestriction.defaultRestriction();
        }
        Set<String> allowedLanguageNames = LanguageRestrictionAdapter.getFinalAllowedLanguageNames(ImmutableList.of(languageRestriction));

        boolean isAllowedToSubmitByPartner = ProgrammingProblemControllerUtils.isAllowedToSubmit(problemService, problem);
        boolean isClean = !problemService.userCloneExists(IdentityUtils.getUserJid(), problem.getJid());

        String reasonNotAllowedToSubmit = null;

        if (!isAllowedToSubmitByPartner) {
            reasonNotAllowedToSubmit = Messages.get("problem.programming.cantSubmit");
        } else if (!isClean) {
            reasonNotAllowedToSubmit = Messages.get("problem.programming.cantSubmitNotClean");
        }

        LazyHtml content = new LazyHtml(GradingEngineAdapterRegistry.getInstance().getByGradingEngineName(engine).renderViewStatement(routes.ProgrammingProblemSubmissionController.postSubmit(problemId).absoluteURL(request(), request().secure()), statement, config, engine, allowedLanguageNames, reasonNotAllowedToSubmit));

        Set<String> allowedLanguages;
        try {
            allowedLanguages = ProblemControllerUtils.getAllowedLanguagesToView(problemService, problem);
        } catch (IOException e) {
            return notFound();
        }

        ProblemControllerUtils.appendStatementLanguageSelectionLayout(content, ProblemControllerUtils.getCurrentStatementLanguage(), allowedLanguages, routes.ProblemController.switchLanguage(problem.getId()));

        ProblemStatementControllerUtils.appendSubtabsLayout(content, problemService, problem);
        ProgrammingProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        SandalphonControllerUtils.getInstance().appendSidebarLayout(content);
        ProblemStatementControllerUtils.appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.statement.view"), routes.ProblemStatementController.viewStatement(problemId)));
        SandalphonControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Statement");

        SandalphonControllerUtils.getInstance().addActivityLog("View statement of programming problem " + problem.getSlug() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return SandalphonControllerUtils.getInstance().lazyOk(content);
    }
}
