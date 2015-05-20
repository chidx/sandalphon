package org.iatoki.judgels.sandalphon.models.daos.interfaces;

import org.iatoki.judgels.commons.models.daos.interfaces.Dao;
import org.iatoki.judgels.sandalphon.models.domains.ClientProblemModel;

import java.util.List;

public interface ClientProblemDao extends Dao<Long, ClientProblemModel> {

    boolean existsByClientJidAndProblemJid(String clientJid, String problemJid);

    ClientProblemModel findByClientJidAndProblemJid(String clientJid, String problemJid);

    List<ClientProblemModel> findByProblemJid(String problemJid);
}
