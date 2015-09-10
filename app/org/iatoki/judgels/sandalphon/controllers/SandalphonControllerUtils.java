package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.jophiel.UserActivityMessage;
import org.iatoki.judgels.api.jophiel.JophielClientAPI;
import org.iatoki.judgels.api.jophiel.JophielPublicAPI;
import org.iatoki.judgels.jophiel.controllers.JophielClientControllerUtils;
import org.iatoki.judgels.jophiel.services.impls.UserActivityMessageServiceImpl;
import org.iatoki.judgels.jophiel.forms.SearchProfileForm;
import org.iatoki.judgels.jophiel.forms.ViewpointForm;
import org.iatoki.judgels.jophiel.views.html.client.linkedClientsLayout;
import org.iatoki.judgels.jophiel.views.html.isLoggedInLayout;
import org.iatoki.judgels.jophiel.views.html.isLoggedOutLayout;
import org.iatoki.judgels.jophiel.views.html.profile.searchProfileLayout;
import org.iatoki.judgels.jophiel.views.html.viewas.viewAsLayout;
import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.InternalLink;
import org.iatoki.judgels.play.JudgelsPlayUtils;
import org.iatoki.judgels.play.LazyHtml;
import org.iatoki.judgels.play.controllers.AbstractJudgelsControllerUtils;
import org.iatoki.judgels.play.controllers.ControllerUtils;
import org.iatoki.judgels.play.views.html.layouts.menusLayout;
import org.iatoki.judgels.play.views.html.layouts.profileView;
import org.iatoki.judgels.play.views.html.layouts.sidebarLayout;
import org.iatoki.judgels.sandalphon.SandalphonUtils;
import play.data.Form;
import play.i18n.Messages;
import play.mvc.Http;

public final class SandalphonControllerUtils extends AbstractJudgelsControllerUtils {

    private static SandalphonControllerUtils INSTANCE;

    private final JophielClientAPI jophielClientAPI;
    private final JophielPublicAPI jophielPublicAPI;

    public SandalphonControllerUtils(JophielClientAPI jophielClientAPI, JophielPublicAPI jophielPublicAPI) {
        this.jophielClientAPI = jophielClientAPI;
        this.jophielPublicAPI = jophielPublicAPI;
    }

    @Override
    public void appendSidebarLayout(LazyHtml content) {
        ImmutableList.Builder<InternalLink> internalLinkBuilder = ImmutableList.builder();

        internalLinkBuilder.add(new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()));
        internalLinkBuilder.add(new InternalLink(Messages.get("lesson.lessons"), routes.LessonController.index()));
        if (isAdmin()) {
            internalLinkBuilder.add(new InternalLink(Messages.get("client.clients"), routes.ClientController.index()));
            internalLinkBuilder.add(new InternalLink(Messages.get("grader.graders"), routes.GraderController.index()));
            internalLinkBuilder.add(new InternalLink(Messages.get("user.users"), routes.UserController.index()));
        }

        LazyHtml sidebarContent = new LazyHtml(profileView.render(
                IdentityUtils.getUsername(),
                IdentityUtils.getUserRealName(),
                org.iatoki.judgels.jophiel.controllers.routes.JophielClientController.profile().absoluteURL(Http.Context.current().request(), Http.Context.current().request().secure()),
                org.iatoki.judgels.jophiel.controllers.routes.JophielClientController.logout(routes.ApplicationController.index().absoluteURL(Http.Context.current().request(), Http.Context.current().request().secure())).absoluteURL(Http.Context.current().request(), Http.Context.current().request().secure())
        ));
        if (SandalphonUtils.trullyHasRole("admin")) {
            Form<ViewpointForm> form = Form.form(ViewpointForm.class);
            if (JudgelsPlayUtils.hasViewPoint()) {
                ViewpointForm viewpointForm = new ViewpointForm();
                viewpointForm.username = IdentityUtils.getUsername();
                form.fill(viewpointForm);
            }
            sidebarContent.appendLayout(c -> viewAsLayout.render(form, jophielPublicAPI.getUserAutocompleteAPIEndpoint(), "lib/jophielcommons/javascripts/userAutoComplete.js", org.iatoki.judgels.sandalphon.controllers.routes.ApplicationController.postViewAs(), org.iatoki.judgels.sandalphon.controllers.routes.ApplicationController.resetViewAs(), c));
        }
        sidebarContent.appendLayout(c -> menusLayout.render(internalLinkBuilder.build(), c));
        sidebarContent.appendLayout(c -> linkedClientsLayout.render(jophielClientAPI.getLinkedClientsAPIEndpoint(), "lib/jophielcommons/javascripts/linkedClients.js", c));
        Form<SearchProfileForm> searchProfileForm = Form.form(SearchProfileForm.class);
        sidebarContent.appendLayout(c -> searchProfileLayout.render(searchProfileForm, jophielPublicAPI.getUserAutocompleteAPIEndpoint(), "lib/jophielcommons/javascripts/userAutoComplete.js", JophielClientControllerUtils.getInstance().getUserViewProfileUrl(), c));

        content.appendLayout(c -> sidebarLayout.render(sidebarContent.render(), c));
        if (IdentityUtils.getUserJid() == null) {
            content.appendLayout(c -> isLoggedInLayout.render(jophielClientAPI.getUserIsLoggedInAPIEndpoint(), routes.ApplicationController.auth(ControllerUtils.getCurrentUrl(Http.Context.current().request())).absoluteURL(Http.Context.current().request(), Http.Context.current().request().secure()), "lib/jophielcommons/javascripts/isLoggedIn.js", c));
        } else {
            content.appendLayout(c -> isLoggedOutLayout.render(jophielClientAPI.getUserIsLoggedInAPIEndpoint(), org.iatoki.judgels.jophiel.controllers.routes.JophielClientController.logout(ControllerUtils.getCurrentUrl(Http.Context.current().request())).absoluteURL(Http.Context.current().request(), Http.Context.current().request().secure()), "lib/jophielcommons/javascripts/isLoggedOut.js", c));
        }
    }

    boolean isAdmin() {
        return SandalphonUtils.hasRole("admin");
    }

    void addActivityLog(String log) {
        String newLog = log;
        try {
            if (JudgelsPlayUtils.hasViewPoint()) {
                newLog += " view as " +  IdentityUtils.getUserJid();
            }
            UserActivityMessageServiceImpl.getInstance().addUserActivityMessage(new UserActivityMessage(System.currentTimeMillis(), SandalphonUtils.getRealUserJid(), newLog, IdentityUtils.getIpAddress()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void buildInstance(JophielClientAPI jophielClientAPI, JophielPublicAPI jophielPublicAPI) {
        if (INSTANCE != null) {
            throw new UnsupportedOperationException("SandalphonControllerUtils instance has already been built");
        }
        INSTANCE = new SandalphonControllerUtils(jophielClientAPI, jophielPublicAPI);
    }

    static SandalphonControllerUtils getInstance() {
        if (INSTANCE == null) {
            throw new UnsupportedOperationException("SandalphonControllerUtils instance has not been built");
        }
        return INSTANCE;
    }
}
