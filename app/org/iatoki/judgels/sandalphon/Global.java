package org.iatoki.judgels.sandalphon;

import akka.actor.Scheduler;
import org.iatoki.judgels.commons.GradingResponsePoller;
import org.iatoki.judgels.commons.SubmissionUpdaterService;
import org.iatoki.judgels.gabriel.FakeSealtiel;
import org.iatoki.judgels.sandalphon.controllers.ApplicationController;
import org.iatoki.judgels.sandalphon.controllers.ClientController;
import org.iatoki.judgels.sandalphon.controllers.ProgrammingProblemController;
import org.iatoki.judgels.sandalphon.controllers.UserRoleController;
import org.iatoki.judgels.sandalphon.models.daos.hibernate.ClientHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.hibernate.ClientProblemHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.hibernate.UserRoleHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ClientDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ClientProblemDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.UserRoleDao;
import org.iatoki.judgels.sandalphon.models.daos.programming.hibernate.ProblemHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.programming.hibernate.SubmissionHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.programming.interfaces.ProblemDao;
import org.iatoki.judgels.sandalphon.models.daos.programming.interfaces.SubmissionDao;
import org.iatoki.judgels.sandalphon.programming.ProblemService;
import org.iatoki.judgels.sandalphon.programming.ProblemServiceImpl;
import play.Application;
import play.libs.Akka;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.duration.Duration;

import java.io.File;
import java.util.concurrent.TimeUnit;

public final class Global extends org.iatoki.judgels.commons.Global {

    private final ProblemDao problemDao;
    private final SubmissionDao submissionDao;
    private final ClientDao clientDao;
    private final ClientProblemDao clientProblemDao;
    private final UserRoleDao userRoleDao;
    private final SubmissionUpdaterService submissionUpdaterService;
    private final ProblemService problemService;
    private final ClientService clientService;
    private final UserRoleService userRoleService;
    private final FakeSealtiel sealtiel;

    public Global() {
        this.problemDao = new ProblemHibernateDao();
        this.submissionDao = new SubmissionHibernateDao();
        this.clientDao = new ClientHibernateDao();
        this.clientProblemDao = new ClientProblemHibernateDao();
        this.userRoleDao = new UserRoleHibernateDao();
        this.sealtiel = new FakeSealtiel(new File("/Users/dronjax/grading-requests"), new File("/Users/dronjax/grading-responses"));
        this.problemService = new ProblemServiceImpl(problemDao, submissionDao, sealtiel);
        this.submissionUpdaterService = new SubmissionUpdaterServiceImpl(submissionDao);
        this.clientService = new ClientServiceImpl(clientDao, clientProblemDao);
        this.userRoleService = new UserRoleServiceImpl(userRoleDao);
    }

    @Override
    public void onStart(Application application) {
        super.onStart(application);

        GradingResponsePoller poller = new GradingResponsePoller(submissionUpdaterService, sealtiel);

        Scheduler scheduler = Akka.system().scheduler();
        ExecutionContextExecutor context = Akka.system().dispatcher();
        scheduler.schedule(Duration.create(1, TimeUnit.SECONDS), Duration.create(10, TimeUnit.MILLISECONDS), poller, context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A> A getControllerInstance(Class<A> controllerClass) throws Exception {
         if (controllerClass.equals(ProgrammingProblemController.class)) {
            return (A) new ProgrammingProblemController(problemService, clientService);
         } else if (controllerClass.equals(ClientController.class)) {
            return (A) new ClientController(clientService);
         } else if (controllerClass.equals(ApplicationController.class)) {
            return (A) new ApplicationController(userRoleService);
         } else if (controllerClass.equals(UserRoleController.class)) {
            return (A) new UserRoleController(userRoleService);
         } else {
            return controllerClass.newInstance();
         }
    }
}
