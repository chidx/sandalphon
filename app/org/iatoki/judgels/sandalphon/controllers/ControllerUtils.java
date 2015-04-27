package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.InternalLink;
import org.iatoki.judgels.commons.LazyHtml;
import org.iatoki.judgels.commons.controllers.AbstractControllerUtils;
import org.iatoki.judgels.commons.views.html.layouts.sidebarLayout;
import org.iatoki.judgels.sandalphon.views.html.layouts.viewAsLayout;
import org.iatoki.judgels.jophiel.commons.UserActivity;
import org.iatoki.judgels.sandalphon.SandalphonUtils;
import org.iatoki.judgels.sandalphon.UserActivityServiceImpl;
import org.iatoki.judgels.sandalphon.UserViewpointForm;
import play.data.Form;
import play.i18n.Messages;
import play.mvc.Http;

public final class ControllerUtils extends AbstractControllerUtils {

    private static ControllerUtils INSTANCE = new ControllerUtils();

    @Override
    public void appendTemplateLayout(LazyHtml content, String title) {
        if (SandalphonUtils.trullyHasRole("admin")) {
            Form<UserViewpointForm> form = Form.form(UserViewpointForm.class);
            if (SandalphonUtils.hasViewPoint()) {
                UserViewpointForm userViewpointForm = new UserViewpointForm();
                userViewpointForm.username = IdentityUtils.getUsername();
                form.fill(userViewpointForm);
            }
            content.appendLayout(c -> viewAsLayout.render(form, c));
        }
        super.appendTemplateLayout(content, title);
    }

    @Override
    public void appendSidebarLayout(LazyHtml content) {
        ImmutableList.Builder<InternalLink> internalLinks = ImmutableList.builder();

        internalLinks.add(new InternalLink(Messages.get("problem.problems"), routes.ProblemController.index()));
        if (isAdmin()) {
            internalLinks.add(new InternalLink(Messages.get("client.clients"), routes.ClientController.index()));
            internalLinks.add(new InternalLink(Messages.get("grader.graders"), routes.GraderController.index()));
            internalLinks.add(new InternalLink(Messages.get("user.users"), routes.UserController.index()));
        }
        content.appendLayout(c -> sidebarLayout.render(
                        IdentityUtils.getUsername(),
                        IdentityUtils.getUserRealName(),
                        org.iatoki.judgels.jophiel.commons.controllers.routes.JophielClientController.profile(org.iatoki.judgels.sandalphon.controllers.routes.ApplicationController.afterProfile(routes.ProblemController.index().absoluteURL(Http.Context.current().request(), Http.Context.current().request().secure())).absoluteURL(Http.Context.current().request(), Http.Context.current().request().secure())).absoluteURL(Http.Context.current().request(), Http.Context.current().request().secure()),
                        org.iatoki.judgels.jophiel.commons.controllers.routes.JophielClientController.logout(routes.ApplicationController.index().absoluteURL(Http.Context.current().request(), Http.Context.current().request().secure())).absoluteURL(Http.Context.current().request(), Http.Context.current().request().secure()),
                        internalLinks.build(), c)
        );
    }

    boolean isAdmin() {
        return SandalphonUtils.hasRole("admin");
    }

    public void addActivityLog(String log) {
        try {
            UserActivityServiceImpl.getInstance().addUserActivity(new UserActivity(System.currentTimeMillis(), IdentityUtils.getUserJid(), log, IdentityUtils.getIpAddress()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static ControllerUtils getInstance() {
        return INSTANCE;
    }
}
