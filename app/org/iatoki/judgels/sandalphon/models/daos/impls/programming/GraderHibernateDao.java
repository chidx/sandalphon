package org.iatoki.judgels.sandalphon.models.daos.impls.programming;

import com.google.common.collect.ImmutableList;
import org.iatoki.judgels.commons.models.daos.hibernate.AbstractJudgelsHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.programming.GraderDao;
import org.iatoki.judgels.sandalphon.models.entities.programming.GraderModel;
import org.iatoki.judgels.sandalphon.models.entities.programming.GraderModel_;

import javax.persistence.metamodel.SingularAttribute;
import java.util.List;

public final class GraderHibernateDao extends AbstractJudgelsHibernateDao<GraderModel> implements GraderDao {

    public GraderHibernateDao() {
        super(GraderModel.class);
    }

    @Override
    protected List<SingularAttribute<GraderModel, String>> getColumnsFilterableByString() {
        return ImmutableList.of(GraderModel_.name);
    }
}