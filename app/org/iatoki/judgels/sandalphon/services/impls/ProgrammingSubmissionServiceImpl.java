package org.iatoki.judgels.sandalphon.services.impls;

import org.iatoki.judgels.sandalphon.config.GabrielClientJid;
import org.iatoki.judgels.sandalphon.models.daos.programming.ProgrammingGradingDao;
import org.iatoki.judgels.sandalphon.models.daos.programming.ProgrammingSubmissionDao;
import org.iatoki.judgels.sandalphon.models.entities.programming.ProgrammingGradingModel;
import org.iatoki.judgels.sandalphon.models.entities.programming.ProgrammingSubmissionModel;
import org.iatoki.judgels.sandalphon.services.ProgrammingSubmissionService;
import org.iatoki.judgels.sealtiel.Sealtiel;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named("programmingSubmissionService")
public final class ProgrammingSubmissionServiceImpl extends AbstractProgrammingSubmissionServiceImpl<ProgrammingSubmissionModel, ProgrammingGradingModel> implements ProgrammingSubmissionService {

    @Inject
    public ProgrammingSubmissionServiceImpl(ProgrammingSubmissionDao submissionDao, ProgrammingGradingDao programmingGradingDao, Sealtiel sealtiel, @GabrielClientJid String gabrielClientJid) {
        super(submissionDao, programmingGradingDao, sealtiel, gabrielClientJid);
    }
}
