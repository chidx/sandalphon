package org.iatoki.judgels.sandalphon.services.impls;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.JudgelsPlayUtils;
import org.iatoki.judgels.play.Page;
import org.iatoki.judgels.sandalphon.Client;
import org.iatoki.judgels.sandalphon.ClientLesson;
import org.iatoki.judgels.sandalphon.ClientNotFoundException;
import org.iatoki.judgels.sandalphon.ClientProblem;
import org.iatoki.judgels.sandalphon.models.daos.ClientDao;
import org.iatoki.judgels.sandalphon.models.daos.ClientLessonDao;
import org.iatoki.judgels.sandalphon.models.daos.ClientProblemDao;
import org.iatoki.judgels.sandalphon.models.entities.ClientLessonModel;
import org.iatoki.judgels.sandalphon.models.entities.ClientModel;
import org.iatoki.judgels.sandalphon.models.entities.ClientProblemModel;
import org.iatoki.judgels.sandalphon.services.ClientService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
@Named("clientService")
public final class ClientServiceImpl implements ClientService {

    private final ClientDao clientDao;
    private final ClientLessonDao clientLessonDao;
    private final ClientProblemDao clientProblemDao;

    @Inject
    public ClientServiceImpl(ClientDao clientDao, ClientLessonDao clientLessonDao, ClientProblemDao clientProblemDao) {
        this.clientDao = clientDao;
        this.clientLessonDao = clientLessonDao;
        this.clientProblemDao = clientProblemDao;
    }

    @Override
    public boolean clientExistsByJid(String clientJid) {
        return clientDao.existsByJid(clientJid);
    }

    @Override
    public List<Client> getClients() {
        List<ClientModel> clientModels = clientDao.getAll();

        return clientModels.stream().map(c -> new Client(c.id, c.jid, c.name, c.secret)).collect(Collectors.toList());
    }

    @Override
    public Client findClientById(long clientId) throws ClientNotFoundException {
        ClientModel clientModel = clientDao.findById(clientId);
        if (clientModel == null) {
            throw new ClientNotFoundException("Client not found.");
        }

        return createClientFromModel(clientModel);
    }


    @Override
    public Client findClientByJid(String clientJid) {
        ClientModel clientModel = clientDao.findByJid(clientJid);

        return createClientFromModel(clientModel);
    }

    @Override
    public void createClient(String name) {
        ClientModel clientModel = new ClientModel();
        clientModel.name = name;
        clientModel.secret = JudgelsPlayUtils.generateNewSecret();

        clientDao.persist(clientModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public void updateClient(long clientId, String name) {
        ClientModel clientModel = clientDao.findById(clientId);
        clientModel.name = name;

        clientDao.edit(clientModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public void deleteClient(long clientId) {
        ClientModel clientModel = clientDao.findById(clientId);

        clientDao.remove(clientModel);
    }

    @Override
    public Page<Client> getPageOfClients(long pageIndex, long pageSize, String orderBy, String orderDir, String filterString) {
        long totalPages = clientDao.countByFilters(filterString, ImmutableMap.of(), ImmutableMap.of());
        List<ClientModel> clientModels = clientDao.findSortedByFilters(orderBy, orderDir, filterString, ImmutableMap.of(), ImmutableMap.of(), pageIndex * pageSize, pageSize);

        List<Client> clients = Lists.transform(clientModels, m -> createClientFromModel(m));

        return new Page<>(clients, totalPages, pageIndex, pageSize);
    }

    @Override
    public boolean isClientAuthorizedForProblem(String problemJid, String clientJid) {
        return clientProblemDao.existsByClientJidAndProblemJid(clientJid, problemJid);
    }

    @Override
    public ClientProblem findClientProblemByClientJidAndProblemJid(String clientJid, String problemJid) {
        ClientProblemModel clientProblemModel = clientProblemDao.findByClientJidAndProblemJid(clientJid, problemJid);
        ClientModel clientModel = clientDao.findByJid(clientProblemModel.clientJid);

        return new ClientProblem(clientProblemModel.id, clientProblemModel.clientJid, clientModel.name, clientProblemModel.problemJid, clientProblemModel.secret);
    }

    @Override
    public ClientProblem findClientProblemById(long clientProblemId) {
        ClientProblemModel clientProblemModel = clientProblemDao.findById(clientProblemId);
        ClientModel clientModel = clientDao.findByJid(clientProblemModel.clientJid);

        return new ClientProblem(clientProblemModel.id, clientProblemModel.clientJid, clientModel.name, clientProblemModel.problemJid, clientProblemModel.secret);
    }

    @Override
    public List<ClientProblem> getClientProblemsByProblemJid(String problemJid) {
        List<ClientProblemModel> clientProblemModels = clientProblemDao.getByProblemJid(problemJid);
        ImmutableList.Builder<ClientProblem> clientProblemBuilder = ImmutableList.builder();

        for (ClientProblemModel clientProblemModel : clientProblemModels) {
            ClientModel clientModel = clientDao.findByJid(clientProblemModel.clientJid);
            clientProblemBuilder.add(new ClientProblem(clientProblemModel.id, clientProblemModel.clientJid, clientModel.name, clientProblemModel.problemJid, clientProblemModel.secret));
        }

        return clientProblemBuilder.build();
    }

    @Override
    public void createClientProblem(String problemJid, String clientJid) {
        ClientProblemModel clientProblemModel = new ClientProblemModel();
        clientProblemModel.problemJid = problemJid;
        clientProblemModel.clientJid = clientJid;
        clientProblemModel.secret = JudgelsPlayUtils.generateNewSecret();

        clientProblemDao.persist(clientProblemModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public void deleteClientProblem(long clientProblemId) {
        ClientProblemModel clientProblemModel = clientProblemDao.findById(clientProblemId);
        clientProblemDao.remove(clientProblemModel);
    }

    @Override
    public boolean isClientAuthorizedForLesson(String lessonJid, String clientJid) {
        return clientLessonDao.existsByClientJidAndLessonJid(clientJid, lessonJid);
    }

    @Override
    public ClientLesson findClientLessonByClientJidAndLessonJid(String clientJid, String lessonJid) {
        ClientLessonModel clientLessonModel = clientLessonDao.findByClientJidAndLessonJid(clientJid, lessonJid);
        ClientModel clientModel = clientDao.findByJid(clientLessonModel.clientJid);

        return new ClientLesson(clientLessonModel.id, clientLessonModel.clientJid, clientModel.name, clientLessonModel.lessonJid, clientLessonModel.secret);
    }

    @Override
    public ClientLesson findClientLessonById(long clientLessonId) {
        ClientLessonModel clientLessonModel = clientLessonDao.findById(clientLessonId);
        ClientModel clientModel = clientDao.findByJid(clientLessonModel.clientJid);

        return new ClientLesson(clientLessonModel.id, clientLessonModel.clientJid, clientModel.name, clientLessonModel.lessonJid, clientLessonModel.secret);
    }

    @Override
    public List<ClientLesson> getClientLessonsByLessonJid(String lessonJid) {
        List<ClientLessonModel> clientLessonModels = clientLessonDao.getByLessonJid(lessonJid);
        ImmutableList.Builder<ClientLesson> clientLessonBuilder = ImmutableList.builder();

        for (ClientLessonModel clientLessonModel : clientLessonModels) {
            ClientModel clientModel = clientDao.findByJid(clientLessonModel.clientJid);
            clientLessonBuilder.add(new ClientLesson(clientLessonModel.id, clientLessonModel.clientJid, clientModel.name, clientLessonModel.lessonJid, clientLessonModel.secret));
        }

        return clientLessonBuilder.build();
    }

    @Override
    public void createClientLesson(String lessonJid, String clientJid) {
        ClientLessonModel clientLessonModel = new ClientLessonModel();
        clientLessonModel.lessonJid = lessonJid;
        clientLessonModel.clientJid = clientJid;
        clientLessonModel.secret = JudgelsPlayUtils.generateNewSecret();

        clientLessonDao.persist(clientLessonModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public void deleteClientLesson(long clientLessonId) {
        ClientLessonModel clientLessonModel = clientLessonDao.findById(clientLessonId);
        clientLessonDao.remove(clientLessonModel);
    }

    private Client createClientFromModel(ClientModel clientModel) {
        return new Client(clientModel.id, clientModel.jid, clientModel.name, clientModel.secret);
    }
}
