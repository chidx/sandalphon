package org.iatoki.judgels.sandalphon.controllers.api.internal;

import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.controllers.apis.AbstractJudgelsAPIController;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemNotFoundException;
import org.iatoki.judgels.sandalphon.controllers.SandalphonControllerUtils;
import org.iatoki.judgels.sandalphon.controllers.securities.Authenticated;
import org.iatoki.judgels.sandalphon.controllers.securities.HasRole;
import org.iatoki.judgels.sandalphon.controllers.securities.LoggedIn;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import play.db.jpa.Transactional;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named
@Authenticated(value = {LoggedIn.class, HasRole.class})
public final class InternalProblemStatementAPIController extends AbstractJudgelsAPIController {

    private final ProblemService problemService;

    @Inject
    public InternalProblemStatementAPIController(ProblemService problemService) {
        this.problemService = problemService;
    }

    @Transactional(readOnly = true)
    public Result renderMediaById(long problemId, String mediaFilename) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(problemId);
        String mediaUrl = problemService.getStatementMediaFileURL(IdentityUtils.getUserJid(), problem.getJid(), mediaFilename);

        return okAsImage(mediaUrl);
    }

    @Transactional(readOnly = true)
    public Result downloadStatementMediaFile(long id, String filename) throws ProblemNotFoundException {
        Problem problem = problemService.findProblemById(id);
        String mediaUrl = problemService.getStatementMediaFileURL(IdentityUtils.getUserJid(), problem.getJid(), filename);

        SandalphonControllerUtils.getInstance().addActivityLog("Download media file " + filename + " of problem " + problem.getSlug() + " <a href=\"" + "http://" + Http.Context.current().request().host() + Http.Context.current().request().uri() + "\">link</a>.");

        return okAsDownload(mediaUrl);
    }
}
