package org.iatoki.judgels.sandalphon.models.daos.hibernate;

import org.iatoki.judgels.commons.models.daos.hibernate.AbstractJidCacheHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.JidCacheDao;
import org.iatoki.judgels.sandalphon.models.domains.JidCacheModel;

public final class JidCacheHibernateDao extends AbstractJidCacheHibernateDao<JidCacheModel> implements JidCacheDao {
    public JidCacheHibernateDao() {
        super(JidCacheModel.class);
    }

    @Override
    public JidCacheModel createJidCacheModel() {
        return new JidCacheModel();
    }
}
