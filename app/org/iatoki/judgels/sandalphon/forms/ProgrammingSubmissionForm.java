package org.iatoki.judgels.sandalphon.forms;

import play.data.validation.Constraints;

import java.io.File;

public final class ProgrammingSubmissionForm {

    @Constraints.Required
    public File file;
}
