package org.iatoki.judgels.sandalphon.controllers;

import org.iatoki.judgels.play.InternalLink;
import org.iatoki.judgels.play.LazyHtml;
import org.iatoki.judgels.play.controllers.BaseController;
import org.iatoki.judgels.sandalphon.Client;
import org.iatoki.judgels.sandalphon.ClientProblem;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemNotFoundException;
import org.iatoki.judgels.sandalphon.controllers.securities.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.securities.HasRole;
import org.iatoki.judgels.sandalphon.controllers.securities.LoggedIn;
import org.iatoki.judgels.sandalphon.forms.ClientProblemUpsertForm;
import org.iatoki.judgels.sandalphon.services.ClientService;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import org.iatoki.judgels.sandalphon.views.html.problem.client.updateClientProblemsView;
import org.iatoki.judgels.sandalphon.views.html.problem.client.viewClientProblemView;
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
import java.util.List;

@Authenticated(value = {LoggedIn.class, HasRole.class})
@Singleton
@Named
public final class ProblemClientController extends BaseController {

    private final ProblemService problemService;
    private final ClientService clientService;

    @Inject
    public ProblemClientController(ProblemService problemService, ClientService clientService) {
        this.problemService = problemService;
        this.clientService = clientService;
    }

    @Transactional(readOnly = true)
    @AddCSRFToken
    public Result updateClientProblems(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (ProblemControllerUtils.isAllowedToManageClients(problemService, problem)) {
            Form<ClientProblemUpsertForm> form = Form.form(ClientProblemUpsertForm.class);
            List<ClientProblem> clientProblems = clientService.findAllClientProblemByProblemJid(problem.getJid());
            List<Client> clients = clientService.findAllClients();

            ControllerUtils.getInstance().addActivityLog("Try to update client on problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return showUpdateClientProblems(form, problem, clients, clientProblems);
        } else {
            return notFound();
        }
    }

    @Transactional
    @RequireCSRFCheck
    public Result postUpdateClientProblems(long problemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);

        if (ProblemControllerUtils.isAllowedToManageClients(problemService, problem)) {
            Form<ClientProblemUpsertForm> form = Form.form(ClientProblemUpsertForm.class).bindFromRequest();

            if (form.hasErrors() || form.hasGlobalErrors()) {
                List<ClientProblem> clientProblems = clientService.findAllClientProblemByProblemJid(problem.getJid());
                List<Client> clients = clientService.findAllClients();
                return showUpdateClientProblems(form, problem, clients, clientProblems);
            } else {
                ClientProblemUpsertForm clientProblemUpsertForm = form.get();
                if ((clientService.clientExistsByClientJid(clientProblemUpsertForm.clientJid)) && (!clientService.isClientProblemInProblemByClientJid(problem.getJid(), clientProblemUpsertForm.clientJid))) {
                    clientService.createClientProblem(problem.getJid(), clientProblemUpsertForm.clientJid);

                    ControllerUtils.getInstance().addActivityLog("Add client " + clientProblemUpsertForm.clientJid + " to problem " + problem.getName() + ".");

                    return redirect(routes.ProblemClientController.updateClientProblems(problem.getId()));
                } else {
                    List<ClientProblem> clientProblems = clientService.findAllClientProblemByProblemJid(problem.getJid());
                    List<Client> clients = clientService.findAllClients();
                    return showUpdateClientProblems(form, problem, clients, clientProblems);
                }
            }
        } else {
            return notFound();
        }
    }

    @Transactional(readOnly = true)
    public Result viewClientProblem(long problemId, long clientProblemId) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);
        ClientProblem clientProblem = clientService.findClientProblemByClientProblemId(clientProblemId);
        if (clientProblem.getProblemJid().equals(problem.getJid())) {
            LazyHtml content = new LazyHtml(viewClientProblemView.render(problem, clientProblem));
            ProblemControllerUtils.appendTabsLayout(content, problemService, problem);
            ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
            ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
            ControllerUtils.getInstance().appendSidebarLayout(content);
            appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.client.client"), routes.ProblemClientController.viewClientProblem(problemId, clientProblemId)));
            ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Statement");

            ControllerUtils.getInstance().addActivityLog("View client " + clientProblem.getClientName() + " to problem " + problem.getName() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

            return ControllerUtils.getInstance().lazyOk(content);
        } else {
            return notFound();
        }
    }

    private Result showUpdateClientProblems(Form<ClientProblemUpsertForm> form, Problem problem, List<Client> clients, List<ClientProblem> clientProblems) {
        LazyHtml content = new LazyHtml(updateClientProblemsView.render(form, problem.getId(), clients, clientProblems));
        ProblemControllerUtils.appendTabsLayout(content, problemService, problem);
        ProblemControllerUtils.appendVersionLocalChangesWarningLayout(content, problemService, problem);
        ProblemControllerUtils.appendTitleLayout(content, problemService, problem);
        ControllerUtils.getInstance().appendSidebarLayout(content);
        appendBreadcrumbsLayout(content, problem, new InternalLink(Messages.get("problem.client.list"), routes.ProblemClientController.updateClientProblems(problem.getId())));
        ControllerUtils.getInstance().appendTemplateLayout(content, "Problem - Update Client");

        return ControllerUtils.getInstance().lazyOk(content);
    }

    private void appendBreadcrumbsLayout(LazyHtml content, Problem problem, InternalLink lastLink) {
        ControllerUtils.getInstance().appendBreadcrumbsLayout(content,
                ProblemControllerUtils.getProblemBreadcrumbsBuilder(problem)
                .add(new InternalLink(Messages.get("problem.client"), routes.ProblemController.jumpToClients(problem.getId())))
                .add(lastLink)
                .build()
        );
    }
}
