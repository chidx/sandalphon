package org.iatoki.judgels.sandalphon.models.daos.hibernate;

import org.iatoki.judgels.commons.models.daos.hibernate.AbstractHibernateDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ClientProblemDao;
import org.iatoki.judgels.sandalphon.models.domains.ClientProblemModel;
import org.iatoki.judgels.sandalphon.models.domains.ClientProblemModel_;
import play.db.jpa.JPA;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

public final class ClientProblemHibernateDao extends AbstractHibernateDao<Long, ClientProblemModel> implements ClientProblemDao {

    @Override
    public boolean isExistByClientJid(String problemJid, String clientJid) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<ClientProblemModel> root = query.from(ClientProblemModel.class);

        query
                .select(cb.count(root))
                .where(cb.and(cb.equal(root.get(ClientProblemModel_.problemJid), problemJid), cb.equal(root.get(ClientProblemModel_.clientJid), clientJid)));

        return (JPA.em().createQuery(query).getSingleResult() != 0);
    }

    @Override
    public List<ClientProblemModel> findByProblemJid(String problemJid) {
        CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
        CriteriaQuery<ClientProblemModel> query = cb.createQuery(ClientProblemModel.class);
        Root<ClientProblemModel> root = query.from(ClientProblemModel.class);

        query
            .where(cb.equal(root.get(ClientProblemModel_.problemJid), problemJid));

        return JPA.em().createQuery(query).getResultList();
    }
}