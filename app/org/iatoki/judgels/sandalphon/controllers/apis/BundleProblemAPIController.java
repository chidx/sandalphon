package org.iatoki.judgels.sandalphon.controllers.apis;

import com.google.gson.Gson;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.iatoki.judgels.commons.JudgelsUtils;
import org.iatoki.judgels.sandalphon.BundleProblemGraderImpl;
import org.iatoki.judgels.sandalphon.Client;
import org.iatoki.judgels.sandalphon.ClientProblem;
import org.iatoki.judgels.sandalphon.services.ClientService;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.services.ProblemService;
import org.iatoki.judgels.sandalphon.BundleAnswer;
import org.iatoki.judgels.sandalphon.BundleGradingResult;
import play.data.DynamicForm;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;

import java.io.IOException;

@Transactional
public final class BundleProblemAPIController extends Controller {
    private final ProblemService problemService;
    private final ClientService clientService;
    private final BundleProblemGraderImpl bundleProblemGrader;

    public BundleProblemAPIController(ProblemService problemService, ClientService clientService, BundleProblemGraderImpl bundleProblemGrader) {
        this.problemService = problemService;
        this.clientService = clientService;
        this.bundleProblemGrader = bundleProblemGrader;
    }

    public Result gradeProblem() {
        UsernamePasswordCredentials credentials = JudgelsUtils.parseBasicAuthFromRequest(request());

        if (credentials != null) {
            DynamicForm form = DynamicForm.form().bindFromRequest();

            String clientJid = credentials.getUserName();
            String clientSecret = credentials.getPassword();
            String problemJid = form.get("problemJid");

            try {
                response().setHeader("Access-Control-Allow-Origin", "*");
                if ((!clientService.clientExistsByClientJid(clientJid)) && (!problemService.problemExistsByJid(problemJid)) && (!clientService.isClientProblemInProblemByClientJid(problemJid, clientJid))) {
                    return notFound();
                }

                Client client = clientService.findClientByJid(clientJid);
                Problem problem = problemService.findProblemByJid(problemJid);
                ClientProblem clientProblem = clientService.findClientProblemByClientJidAndProblemJid(clientJid, problemJid);

                if (!client.getSecret().equals(clientSecret)) {
                    return forbidden();
                }

                BundleAnswer bundleAnswer = new Gson().fromJson(form.get("answer"), BundleAnswer.class);
                BundleGradingResult gradingResult = bundleProblemGrader.gradeBundleProblem(problem.getJid(), bundleAnswer);

                return ok(new Gson().toJson(gradingResult));
            } catch (IOException e) {
                return notFound();
            }
        } else {
            response().setHeader("WWW-Authenticate", "Basic realm=\"" + request().host() + "\"");
            return unauthorized();
        }
    }

}
