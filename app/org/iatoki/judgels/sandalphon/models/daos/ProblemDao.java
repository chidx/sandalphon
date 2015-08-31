package org.iatoki.judgels.sandalphon.models.daos;

import org.iatoki.judgels.play.models.daos.JudgelsDao;
import org.iatoki.judgels.sandalphon.models.entities.ProblemModel;

import java.util.List;

public interface ProblemDao extends JudgelsDao<ProblemModel> {

    List<String> getJidsByAuthorJid(String authorJid);

    boolean existsBySlug(String slug);
}
