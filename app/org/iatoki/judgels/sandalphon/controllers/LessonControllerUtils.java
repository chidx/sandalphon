package org.iatoki.judgels.sandalphon.controllers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.InternalLink;
import org.iatoki.judgels.play.LazyHtml;
import org.iatoki.judgels.play.views.html.layouts.headingWithActionLayout;
import org.iatoki.judgels.play.views.html.layouts.tabLayout;
import org.iatoki.judgels.sandalphon.Lesson;
import org.iatoki.judgels.sandalphon.LessonPartnerConfig;
import org.iatoki.judgels.sandalphon.services.LessonService;
import org.iatoki.judgels.sandalphon.StatementLanguageStatus;
import org.iatoki.judgels.sandalphon.views.html.problem.statement.statementLanguageSelectionLayout;
import org.iatoki.judgels.sandalphon.views.html.lesson.version.versionLocalChangesWarningLayout;
import play.i18n.Messages;
import play.mvc.Call;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class LessonControllerUtils {

    private LessonControllerUtils() {
        // prevent instantiation
    }

    static void appendTabsLayout(LazyHtml content, LessonService lessonService, Lesson lesson) {
        ImmutableList.Builder<InternalLink> internalLinks = ImmutableList.builder();

        internalLinks.add(new InternalLink(Messages.get("lesson.statement"), routes.LessonController.jumpToStatement(lesson.getId())));

        if (LessonControllerUtils.isAuthorOrAbove(lesson)) {
            internalLinks.add(new InternalLink(Messages.get("lesson.partner"), routes.LessonController.jumpToPartners(lesson.getId())));
        }

        internalLinks.add(new InternalLink(Messages.get("lesson.version"), routes.LessonController.jumpToVersions(lesson.getId())));

        if (LessonControllerUtils.isAllowedToManageClients(lessonService, lesson)) {
            internalLinks.add(new InternalLink(Messages.get("lesson.client"), routes.LessonController.jumpToClients(lesson.getId())));
        }

        content.appendLayout(c -> tabLayout.render(internalLinks.build(), c));
    }

    static void appendTitleLayout(LazyHtml content, LessonService lessonService, Lesson lesson) {
        if (isAllowedToUpdateLesson(lessonService, lesson)) {
            content.appendLayout(c -> headingWithActionLayout.render("#" + lesson.getId() + ": " + lesson.getName(), new InternalLink(Messages.get("lesson.update"), routes.LessonController.updateLesson(lesson.getId())), c));
        } else {
            content.appendLayout(c -> headingWithActionLayout.render("#" + lesson.getId() + ": " + lesson.getName(), new InternalLink(Messages.get("lesson.view"), routes.LessonController.viewLesson(lesson.getId())), c));
        }
    }

    static void appendStatementLanguageSelectionLayout(LazyHtml content, String currentLanguage, Set<String> allowedLanguages, Call target) {
        content.appendLayout(c -> statementLanguageSelectionLayout.render(target.absoluteURL(Controller.request(), Controller.request().secure()), allowedLanguages, currentLanguage, c));
    }

    static void appendVersionLocalChangesWarningLayout(LazyHtml content, LessonService lessonService, Lesson lesson) {
        if (lessonService.userCloneExists(IdentityUtils.getUserJid(), lesson.getJid())) {
            content.appendLayout(c -> versionLocalChangesWarningLayout.render(lesson.getId(), c));
        }
    }

    static void establishStatementLanguage(LessonService lessonService, Lesson lesson) throws IOException {
        String currentLanguage = getCurrentStatementLanguage();
        Map<String, StatementLanguageStatus> availableLanguages = lessonService.getAvailableLanguages(IdentityUtils.getUserJid(), lesson.getJid());

        if (currentLanguage == null || !availableLanguages.containsKey(currentLanguage) || availableLanguages.get(currentLanguage) == StatementLanguageStatus.DISABLED) {
            String languageCode = lessonService.getDefaultLanguage(IdentityUtils.getUserJid(), lesson.getJid());
            setCurrentStatementLanguage(languageCode);
        }
    }

    static String getDefaultStatementLanguage(LessonService lessonService, Lesson lesson) throws IOException {
        return lessonService.getDefaultLanguage(IdentityUtils.getUserJid(), lesson.getJid());
    }

    static void setCurrentStatementLanguage(String languageCode) {
        Controller.session("currentStatementLanguage", languageCode);
    }

    static String getCurrentStatementLanguage() {
        return Controller.session("currentStatementLanguage");
    }

    static ImmutableList.Builder<InternalLink> getLessonBreadcrumbsBuilder(Lesson lesson) {
        ImmutableList.Builder<InternalLink> internalLinks = ImmutableList.builder();
        internalLinks
                .add(new InternalLink(Messages.get("lesson.lessons"), routes.LessonController.index()))
                .add(new InternalLink(lesson.getName(), routes.LessonController.enterLesson(lesson.getId())));

        return internalLinks;
    }

    static Result downloadFile(File file) {
        if (!file.exists()) {
            return Results.notFound();
        }
        Controller.response().setContentType("application/x-download");
        Controller.response().setHeader("Content-disposition", "attachment; filename=" + file.getName());
        return Results.ok(file);
    }

    static boolean isAuthor(Lesson lesson) {
        return lesson.getAuthorJid().equals(IdentityUtils.getUserJid());
    }

    static boolean isAuthorOrAbove(Lesson lesson) {
        return SandalphonControllerUtils.getInstance().isAdmin() || isAuthor(lesson);
    }

    static boolean isPartner(LessonService lessonService, Lesson lesson) {
        return lessonService.isUserPartnerForLesson(lesson.getJid(), IdentityUtils.getUserJid());
    }

    static boolean isPartnerOrAbove(LessonService lessonService, Lesson lesson) {
        return isAuthorOrAbove(lesson) || isPartner(lessonService, lesson);
    }

    static boolean isAllowedToUpdateLesson(LessonService lessonService, Lesson lesson) {
        return isAuthorOrAbove(lesson) || (isPartner(lessonService, lesson) && getPartnerConfig(lessonService, lesson).isAllowedToUpdateLesson());
    }

    static boolean isAllowedToUploadStatementResources(LessonService lessonService, Lesson lesson) {
        return isAuthorOrAbove(lesson) || (isPartner(lessonService, lesson) && getPartnerConfig(lessonService, lesson).isAllowedToUploadStatementResources());
    }

    static boolean isAllowedToViewStatement(LessonService lessonService, Lesson lesson) {
        if (isAuthorOrAbove(lesson)) {
            return true;
        }

        if (!isPartner(lessonService, lesson)) {
            return false;
        }

        String language = getCurrentStatementLanguage();

        try {
            String defaultLanguage = lessonService.getDefaultLanguage(IdentityUtils.getUserJid(), lesson.getJid());
            Set<String> allowedLanguages = getPartnerConfig(lessonService, lesson).getAllowedStatementLanguagesToView();

            if (allowedLanguages == null || allowedLanguages.contains(language) || language.equals(defaultLanguage)) {
                return true;
            }

            setCurrentStatementLanguage(defaultLanguage);
            return true;
        } catch (IOException e) {
            return false;
        }
    }


    static boolean isAllowedToUpdateStatement(LessonService lessonService, Lesson lesson) {
        return isAuthorOrAbove(lesson) || (isPartner(lessonService, lesson) && getPartnerConfig(lessonService, lesson).isAllowedToUpdateStatement());
    }

    static boolean isAllowedToUpdateStatementInLanguage(LessonService lessonService, Lesson lesson) {
        if (!isAllowedToUpdateStatement(lessonService, lesson)) {
            return false;
        }

        if (isAuthorOrAbove(lesson)) {
            return true;
        }

        if (!isPartner(lessonService, lesson)) {
            return false;
        }

        String language = getCurrentStatementLanguage();

        Set<String> allowedLanguages = getPartnerConfig(lessonService, lesson).getAllowedStatementLanguagesToUpdate();

        if (allowedLanguages == null || allowedLanguages.contains(language)) {
            return true;
        }

        String firstLanguage = allowedLanguages.iterator().next();

        setCurrentStatementLanguage(firstLanguage);
        return true;
    }

    static boolean isAllowedToManageStatementLanguages(LessonService lessonService, Lesson lesson) {
        return isAuthorOrAbove(lesson) || (isPartner(lessonService, lesson) && getPartnerConfig(lessonService, lesson).isAllowedToManageStatementLanguages());
    }

    static boolean isAllowedToViewVersionHistory(LessonService lessonService, Lesson lesson) {
        return isAuthorOrAbove(lesson) || (isPartner(lessonService, lesson) && getPartnerConfig(lessonService, lesson).isAllowedToViewVersionHistory());
    }

    static boolean isAllowedToRestoreVersionHistory(LessonService lessonService, Lesson lesson) {
        return isAuthorOrAbove(lesson) || (isPartner(lessonService, lesson) && getPartnerConfig(lessonService, lesson).isAllowedToRestoreVersionHistory());
    }

    static boolean isAllowedToManageClients(LessonService lessonService, Lesson lesson) {
        return isAuthorOrAbove(lesson) || (isPartner(lessonService, lesson) && getPartnerConfig(lessonService, lesson).isAllowedToManageLessonClients());
    }

    static LessonPartnerConfig getPartnerConfig(LessonService lessonService, Lesson lesson) {
        return lessonService.findLessonPartnerByLessonJidAndPartnerJid(lesson.getJid(), IdentityUtils.getUserJid()).getConfig();
    }

    static Set<String> getAllowedLanguagesToView(LessonService lessonService, Lesson lesson) throws IOException {
        Map<String, StatementLanguageStatus> availableLanguages = lessonService.getAvailableLanguages(IdentityUtils.getUserJid(), lesson.getJid());

        Set<String> allowedLanguages = Sets.newTreeSet();
        allowedLanguages.addAll(availableLanguages.entrySet().stream().filter(e -> e.getValue() == StatementLanguageStatus.ENABLED).map(e -> e.getKey()).collect(Collectors.toSet()));

        if (isPartner(lessonService, lesson)) {
            Set<String> allowedPartnerLanguages = getPartnerConfig(lessonService, lesson).getAllowedStatementLanguagesToView();
            if (allowedPartnerLanguages != null) {
                allowedLanguages.retainAll(allowedPartnerLanguages);
                allowedLanguages.add(lessonService.getDefaultLanguage(IdentityUtils.getUserJid(), lesson.getJid()));
            }
        }

        return ImmutableSet.copyOf(allowedLanguages);
    }

    static Set<String> getAllowedLanguagesToUpdate(LessonService lessonService, Lesson lesson) throws IOException {
        Map<String, StatementLanguageStatus> availableLanguages = lessonService.getAvailableLanguages(IdentityUtils.getUserJid(), lesson.getJid());

        Set<String> allowedLanguages = Sets.newTreeSet();
        allowedLanguages.addAll(availableLanguages.entrySet().stream().filter(e -> e.getValue() == StatementLanguageStatus.ENABLED).map(e -> e.getKey()).collect(Collectors.toSet()));

        if (isPartner(lessonService, lesson) && isAllowedToUpdateStatement(lessonService, lesson)) {
            Set<String> allowedPartnerLanguages = getPartnerConfig(lessonService, lesson).getAllowedStatementLanguagesToUpdate();
            if (allowedPartnerLanguages != null) {
                allowedLanguages.retainAll(allowedPartnerLanguages);
            }
        }

        return ImmutableSet.copyOf(allowedLanguages);
    }
}
