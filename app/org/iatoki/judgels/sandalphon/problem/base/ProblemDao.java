package org.iatoki.judgels.sandalphon.problem.base;

import com.google.inject.ImplementedBy;
import org.iatoki.judgels.play.model.JudgelsDao;

import java.util.List;

@ImplementedBy(ProblemHibernateDao.class)
public interface ProblemDao extends JudgelsDao<ProblemModel> {

    List<String> getJidsByAuthorJid(String authorJid);

    boolean existsBySlug(String slug);

    ProblemModel findBySlug(String slug);
}
