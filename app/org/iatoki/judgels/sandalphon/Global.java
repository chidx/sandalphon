package org.iatoki.judgels.sandalphon;

import akka.actor.Scheduler;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.iatoki.judgels.commons.FileSystemProvider;
import org.iatoki.judgels.commons.GitProvider;
import org.iatoki.judgels.commons.JudgelsProperties;
import org.iatoki.judgels.commons.LocalFileSystemProvider;
import org.iatoki.judgels.commons.LocalGitProvider;
import org.iatoki.judgels.jophiel.services.impls.DefaultUserActivityMessageServiceImpl;
import org.iatoki.judgels.jophiel.Jophiel;
import org.iatoki.judgels.jophiel.UserActivityMessagePusher;
import org.iatoki.judgels.jophiel.controllers.JophielClientController;
import org.iatoki.judgels.sandalphon.services.BundleItemService;
import org.iatoki.judgels.sandalphon.services.impls.BundleItemServiceImpl;
import org.iatoki.judgels.sandalphon.services.BundleProblemService;
import org.iatoki.judgels.sandalphon.BundleProblemGraderImpl;
import org.iatoki.judgels.sandalphon.services.impls.BundleProblemServiceImpl;
import org.iatoki.judgels.sandalphon.services.impls.BundleSubmissionServiceImpl;
import org.iatoki.judgels.sandalphon.controllers.ApplicationController;
import org.iatoki.judgels.sandalphon.controllers.BundleItemController;
import org.iatoki.judgels.sandalphon.controllers.BundleProblemController;
import org.iatoki.judgels.sandalphon.controllers.BundleProblemPartnerController;
import org.iatoki.judgels.sandalphon.controllers.BundleProblemStatementController;
import org.iatoki.judgels.sandalphon.controllers.BundleProblemSubmissionController;
import org.iatoki.judgels.sandalphon.controllers.ClientController;
import org.iatoki.judgels.sandalphon.controllers.ControllerUtils;
import org.iatoki.judgels.sandalphon.controllers.GraderController;
import org.iatoki.judgels.sandalphon.controllers.LessonClientController;
import org.iatoki.judgels.sandalphon.controllers.LessonController;
import org.iatoki.judgels.sandalphon.controllers.LessonPartnerController;
import org.iatoki.judgels.sandalphon.controllers.LessonStatementController;
import org.iatoki.judgels.sandalphon.controllers.LessonVersionController;
import org.iatoki.judgels.sandalphon.controllers.ProblemClientController;
import org.iatoki.judgels.sandalphon.controllers.ProblemController;
import org.iatoki.judgels.sandalphon.controllers.ProblemPartnerController;
import org.iatoki.judgels.sandalphon.controllers.ProblemStatementController;
import org.iatoki.judgels.sandalphon.controllers.ProblemVersionController;
import org.iatoki.judgels.sandalphon.controllers.ProgrammingProblemController;
import org.iatoki.judgels.sandalphon.controllers.ProgrammingProblemGradingController;
import org.iatoki.judgels.sandalphon.controllers.ProgrammingProblemPartnerController;
import org.iatoki.judgels.sandalphon.controllers.ProgrammingProblemStatementController;
import org.iatoki.judgels.sandalphon.controllers.ProgrammingProblemSubmissionController;
import org.iatoki.judgels.sandalphon.controllers.UserController;
import org.iatoki.judgels.sandalphon.controllers.apis.BundleProblemAPIController;
import org.iatoki.judgels.sandalphon.controllers.apis.LessonAPIController;
import org.iatoki.judgels.sandalphon.controllers.apis.ProblemAPIController;
import org.iatoki.judgels.sandalphon.controllers.apis.ProgrammingProblemAPIController;
import org.iatoki.judgels.sandalphon.models.daos.impls.ClientHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.impls.ClientLessonHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.impls.ClientProblemHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.impls.JidCacheHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.impls.LessonHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.impls.LessonPartnerHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.impls.ProblemHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.impls.ProblemPartnerHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.impls.UserHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.impls.bundle.BundleGradingHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.impls.bundle.BundleSubmissionHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.impls.programming.GraderHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.impls.programming.GradingHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.impls.programming.ProgrammingSubmissionHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.ClientDao;
import org.iatoki.judgels.sandalphon.models.daos.ClientLessonDao;
import org.iatoki.judgels.sandalphon.models.daos.ClientProblemDao;
import org.iatoki.judgels.sandalphon.models.daos.JidCacheDao;
import org.iatoki.judgels.sandalphon.models.daos.LessonDao;
import org.iatoki.judgels.sandalphon.models.daos.LessonPartnerDao;
import org.iatoki.judgels.sandalphon.models.daos.ProblemDao;
import org.iatoki.judgels.sandalphon.models.daos.ProblemPartnerDao;
import org.iatoki.judgels.sandalphon.models.daos.UserDao;
import org.iatoki.judgels.sandalphon.models.daos.bundle.BundleGradingDao;
import org.iatoki.judgels.sandalphon.models.daos.bundle.BundleSubmissionDao;
import org.iatoki.judgels.sandalphon.models.daos.programming.GraderDao;
import org.iatoki.judgels.sandalphon.models.daos.programming.GradingDao;
import org.iatoki.judgels.sandalphon.models.daos.programming.ProgrammingSubmissionDao;
import org.iatoki.judgels.sandalphon.services.GraderService;
import org.iatoki.judgels.sandalphon.services.impls.GraderServiceImpl;
import org.iatoki.judgels.sandalphon.services.ProgrammingProblemService;
import org.iatoki.judgels.sandalphon.services.impls.ProgrammingProblemServiceImpl;
import org.iatoki.judgels.sandalphon.services.impls.SubmissionServiceImpl;
import org.iatoki.judgels.sandalphon.services.ClientService;
import org.iatoki.judgels.sandalphon.services.JidCacheService;
import org.iatoki.judgels.sandalphon.services.LessonService;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import org.iatoki.judgels.sandalphon.services.UserService;
import org.iatoki.judgels.sandalphon.services.impls.ClientServiceImpl;
import org.iatoki.judgels.sandalphon.services.impls.LessonServiceImpl;
import org.iatoki.judgels.sandalphon.services.impls.ProblemServiceImpl;
import org.iatoki.judgels.sandalphon.services.impls.UserActivityMessageServiceImpl;
import org.iatoki.judgels.sandalphon.services.impls.UserServiceImpl;
import org.iatoki.judgels.sealtiel.Sealtiel;
import play.Application;
import play.libs.Akka;
import play.mvc.Controller;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.duration.Duration;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class Global extends org.iatoki.judgels.commons.Global {
    private ClientDao clientDao;
    private ClientProblemDao clientProblemDao;
    private ClientLessonDao clientLessonDao;
    private JidCacheDao jidCacheDao;
    private ProblemDao problemDao;
    private ProblemPartnerDao problemPartnerDao;
    private LessonDao lessonDao;
    private LessonPartnerDao lessonPartnerDao;
    private UserDao userDao;
    private BundleGradingDao bundleGradingDao;
    private BundleSubmissionDao bundleSubmissionDao;
    private GraderDao graderDao;
    private GradingDao gradingDao;
    private ProgrammingSubmissionDao programmingSubmissionDao;

    private SandalphonProperties sandalphonProps;

    private Jophiel jophiel;
    private Sealtiel sealtiel;

    private LocalFileSystemProvider problemFileSystemProvider;
    private FileSystemProvider submissionFileSystemProvider;
    private LocalFileSystemProvider lessonFileSystemProvider;

    private GitProvider problemGitProvider;
    private GitProvider lessonGitProvider;

    private ClientService clientService;
    private GraderService graderService;
    private UserService userService;
    private ProblemService problemService;
    private BundleProblemService bundleProblemService;
    private BundleItemService bundleItemService;
    private BundleSubmissionService bundleSubmissionService;
    private ProgrammingProblemService programmingProblemService;
    private SubmissionService submissionService;
    private LessonService lessonService;

    private Map<Class<?>, Controller> controllersRegistry;

    @Override
    public void onStart(Application application) {
        buildProperties();
        buildDaos();
        buildCommons();
        buildFileProviders();
        buildGitProviders();
        buildServices();
        buildControllers();
        scheduleThreads();
    }

    @Override
    public <A> A getControllerInstance(Class<A> controllerClass) throws Exception {
        return controllerClass.cast(controllersRegistry.get(controllerClass));
    }

    private void buildDaos() {
        clientDao = new ClientHibernateDao();
        clientProblemDao = new ClientProblemHibernateDao();
        clientLessonDao = new ClientLessonHibernateDao();
        jidCacheDao = new JidCacheHibernateDao();
        problemDao = new ProblemHibernateDao();
        problemPartnerDao = new ProblemPartnerHibernateDao();
        lessonDao = new LessonHibernateDao();
        lessonPartnerDao = new LessonPartnerHibernateDao();
        userDao = new UserHibernateDao();
        bundleGradingDao = new BundleGradingHibernateDao();
        bundleSubmissionDao = new BundleSubmissionHibernateDao();
        graderDao = new GraderHibernateDao();
        gradingDao = new GradingHibernateDao();
        programmingSubmissionDao = new ProgrammingSubmissionHibernateDao();
    }

    private void buildProperties() {
        Config config = ConfigFactory.load();

        org.iatoki.judgels.sandalphon.BuildInfo$ buildInfo = org.iatoki.judgels.sandalphon.BuildInfo$.MODULE$;
        JudgelsProperties.buildInstance(buildInfo.name(), buildInfo.version(), config);

        SandalphonProperties.buildInstance(config);
        sandalphonProps = SandalphonProperties.getInstance();
    }

    private void buildCommons() {
        jophiel = new Jophiel(sandalphonProps.getJophielBaseUrl(), sandalphonProps.getJophielClientJid(), sandalphonProps.getJophielClientSecret());
        sealtiel = new Sealtiel(sandalphonProps.getSealtielBaseUrl(), sandalphonProps.getSealtielClientJid(), sandalphonProps.getSealtielClientSecret());
    }

    private void buildFileProviders() {
        problemFileSystemProvider = new LocalFileSystemProvider(sandalphonProps.getProblemLocalDir());
        submissionFileSystemProvider = new LocalFileSystemProvider(sandalphonProps.getSubmissionLocalDir());
        lessonFileSystemProvider = new LocalFileSystemProvider(sandalphonProps.getLessonLocalDir());
    }

    private void buildGitProviders() {
        problemGitProvider = new LocalGitProvider(problemFileSystemProvider);
        lessonGitProvider = new LocalGitProvider(lessonFileSystemProvider);
    }

    private void buildServices() {
        clientService = new ClientServiceImpl(clientDao, clientProblemDao, clientLessonDao);
        graderService = new GraderServiceImpl(graderDao);
        userService = new UserServiceImpl(jophiel, userDao);
        problemService = new ProblemServiceImpl(problemDao, problemPartnerDao, problemFileSystemProvider, problemGitProvider);
        bundleProblemService = new BundleProblemServiceImpl(problemFileSystemProvider);
        bundleItemService = new BundleItemServiceImpl(problemFileSystemProvider);
        bundleSubmissionService = new BundleSubmissionServiceImpl(bundleSubmissionDao, bundleGradingDao, new BundleProblemGraderImpl(problemService, bundleItemService));
        programmingProblemService = new ProgrammingProblemServiceImpl(problemFileSystemProvider);
        submissionService = new SubmissionServiceImpl(programmingSubmissionDao, gradingDao, sealtiel, SandalphonProperties.getInstance().getSealtielGabrielClientJid());
        lessonService = new LessonServiceImpl(lessonDao, lessonPartnerDao, lessonFileSystemProvider, lessonGitProvider);

        JidCacheService.buildInstance(jidCacheDao);
        ControllerUtils.buildInstance(jophiel);
        DefaultUserActivityMessageServiceImpl.buildInstance(jophiel);
    }

    private void buildControllers() {
        controllersRegistry = ImmutableMap.<Class<?>, Controller> builder()
                .put(ApplicationController.class, new ApplicationController(jophiel, userService))
                .put(JophielClientController.class, new JophielClientController(jophiel, userService))
                .put(ClientController.class, new ClientController(clientService))
                .put(GraderController.class, new GraderController(graderService))
                .put(ProblemClientController.class, new ProblemClientController(problemService, clientService))
                .put(ProblemController.class, new ProblemController(problemService))
                .put(ProblemPartnerController.class, new ProblemPartnerController(problemService))
                .put(ProblemStatementController.class, new ProblemStatementController(problemService))
                .put(ProblemVersionController.class, new ProblemVersionController(problemService))
                .put(ProgrammingProblemController.class, new ProgrammingProblemController(problemService, programmingProblemService))
                .put(ProgrammingProblemGradingController.class, new ProgrammingProblemGradingController(problemService, programmingProblemService))
                .put(ProgrammingProblemPartnerController.class, new ProgrammingProblemPartnerController(jophiel, problemService, programmingProblemService))
                .put(ProgrammingProblemStatementController.class, new ProgrammingProblemStatementController(problemService, programmingProblemService))
                .put(ProgrammingProblemSubmissionController.class, new ProgrammingProblemSubmissionController(problemService, programmingProblemService, submissionService, submissionFileSystemProvider))
                .put(BundleProblemController.class, new BundleProblemController(problemService, bundleProblemService))
                .put(BundleProblemStatementController.class, new BundleProblemStatementController(problemService, bundleProblemService, bundleItemService))
                .put(BundleItemController.class, new BundleItemController(problemService, bundleProblemService, bundleItemService))
                .put(BundleProblemPartnerController.class, new BundleProblemPartnerController(jophiel, problemService, bundleProblemService))
                .put(BundleProblemSubmissionController.class, new BundleProblemSubmissionController(problemService, bundleProblemService, bundleSubmissionService, submissionFileSystemProvider))
                .put(ProblemAPIController.class, new ProblemAPIController(problemService, bundleProblemService, bundleItemService, programmingProblemService, clientService))
                .put(BundleProblemAPIController.class, new BundleProblemAPIController(problemService, clientService, new BundleProblemGraderImpl(problemService, bundleItemService)))
                .put(ProgrammingProblemAPIController.class, new ProgrammingProblemAPIController(problemService, programmingProblemService, clientService, graderService))
                .put(LessonClientController.class, new LessonClientController(lessonService, clientService))
                .put(LessonController.class, new LessonController(lessonService))
                .put(LessonPartnerController.class, new LessonPartnerController(jophiel, lessonService))
                .put(LessonStatementController.class, new LessonStatementController(lessonService))
                .put(LessonVersionController.class, new LessonVersionController(lessonService))
                .put(LessonAPIController.class, new LessonAPIController(lessonService, clientService))
                .put(UserController.class, new UserController(jophiel, userService))
                .build();
    }

    private void scheduleThreads() {
        Scheduler scheduler = Akka.system().scheduler();
        ExecutionContextExecutor context = Akka.system().dispatcher();

        GradingResponsePoller poller = new GradingResponsePoller(scheduler, context, submissionService, sealtiel, TimeUnit.MILLISECONDS.convert(2, TimeUnit.SECONDS));
        UserActivityMessagePusher userActivityMessagePusher = new UserActivityMessagePusher(jophiel, userService, UserActivityMessageServiceImpl.getInstance());

        scheduler.schedule(Duration.create(1, TimeUnit.SECONDS), Duration.create(3, TimeUnit.SECONDS), poller, context);
        scheduler.schedule(Duration.create(1, TimeUnit.SECONDS), Duration.create(1, TimeUnit.MINUTES), userActivityMessagePusher, context);
    }
}
