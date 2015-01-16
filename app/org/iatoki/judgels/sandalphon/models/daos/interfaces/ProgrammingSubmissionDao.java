package org.iatoki.judgels.sandalphon.models.daos.interfaces;

import org.iatoki.judgels.commons.models.daos.interfaces.JudgelsDao;
import org.iatoki.judgels.sandalphon.models.domains.ProgrammingSubmissionModel;

import java.util.List;


public interface ProgrammingSubmissionDao extends JudgelsDao<ProgrammingSubmissionModel> {

    List<ProgrammingSubmissionModel> findByProblem(String problemJid);
}
