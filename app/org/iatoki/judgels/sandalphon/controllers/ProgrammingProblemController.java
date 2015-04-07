package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.views.html.layouts.headingLayout;
import org.iatoki.judgels.sandalphon.ProblemType;
import org.iatoki.judgels.sandalphon.ProblemService;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.programming.ProgrammingProblemStatementUtils;
import org.iatoki.judgels.sandalphon.programming.ProgrammingProblemService;
import org.iatoki.judgels.sandalphon.controllers.security.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.security.HasRole;
import org.iatoki.judgels.sandalphon.controllers.security.LoggedIn;
import org.iatoki.judgels.sandalphon.forms.programming.ProgrammingProblemCreateForm;
import org.iatoki.judgels.sandalphon.views.html.programming.createProgrammingProblemView;
import play.data.Form;
import play.db.jpa.Transactional;
import play.filters.csrf.AddCSRFToken;
import play.filters.csrf.RequireCSRFCheck;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Result;

@Transactional
@Authenticated(value = {LoggedIn.class, HasRole.class})
public final class ProgrammingProblemController extends Controller {

    private final ProblemService problemService;
    private final ProgrammingProblemService programmingProblemService;

    public ProgrammingProblemController(ProblemService problemService, ProgrammingProblemService programmingProblemService) {
        this.problemService = problemService;
        this.programmingProblemService = programmingProblemService;
    }

    @AddCSRFToken
    public Result createProgrammingProblem() {
        if (!ProblemControllerUtils.wasProblemJustCreated()) {
            return badRequest();
        }

        Form<ProgrammingProblemCreateForm> form = Form.form(ProgrammingProblemCreateForm.class);
        return showCreateProgrammingProblem(form);
    }

    @RequireCSRFCheck
    public Result postCreateProgrammingProblem() {
        if (!ProblemControllerUtils.wasProblemJustCreated()) {
            return badRequest();
        }

        Form<ProgrammingProblemCreateForm> form = Form.form(ProgrammingProblemCreateForm.class).bindFromRequest();

        if (form.hasErrors() || form.hasGlobalErrors()) {
            return showCreateProgrammingProblem(form);
        } else {
            ProgrammingProblemCreateForm data = form.get();

            Problem problem = problemService.createProblem(ProblemType.PROGRAMMING, ProblemControllerUtils.getJustCreatedProblemName(), ProblemControllerUtils.getJustCreatedProblemAdditionalNote(), ProblemControllerUtils.getJustCreatedProblemInitLanguageCode());
            problemService.updateStatement(null, problem.getId(), ProblemControllerUtils.getJustCreatedProblemInitLanguageCode(), ProgrammingProblemStatementUtils.getDefaultStatement(ProblemControllerUtils.getJustCreatedProblemInitLanguageCode()));
            programmingProblemService.initProgrammingProblem(problem.getJid(), data.gradingEngineName);
            problemService.initRepository(IdentityUtils.getUserJid(), problem.getJid());

            ProblemControllerUtils.setCurrentStatementLanguage(ProblemControllerUtils.getJustCreatedProblemInitLanguageCode());
            ProblemControllerUtils.removeJustCreatedProblem();
            return redirect(routes.ProblemController.enterProblem(problem.getId()));
        }
    }

    private Result showCreateProgrammingProblem(Form<ProgrammingProblemCreateForm> form) {
        LazyHtml content = new LazyHtml(createProgrammingProblemView.render(form, ProblemControllerUtils.getJustCreatedProblemName(), ProblemControllerUtils.getJustCreatedProblemAdditionalNote(), ProblemControllerUtils.getJustCreatedProblemInitLanguageCode()));
        content.appendLayout(c -> headingLayout.render(Messages.get("problem.programming.create"), c));
        ControllerUtils.getInstance().appendSidebarLayout(content);
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content, ImmutableList.of(
                new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index())
        ));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Programming Problem - Create");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    public Result jumpToGrading(long id) {
        return redirect(routes.ProgrammingProblemGradingController.updateGradingConfig(id));
    }

    public Result jumpToSubmissions(long id) {
        return redirect(routes.ProgrammingProblemSubmissionController.viewSubmissions(id));
    }
}
