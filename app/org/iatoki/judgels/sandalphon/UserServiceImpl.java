package org.iatoki.judgels.sandalphon;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.JudgelsUtils;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.jophiel.commons.Jophiel;
import org.iatoki.judgels.jophiel.commons.UserTokens;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.UserDao;
import org.iatoki.judgels.sandalphon.models.domains.UserModel;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public final class UserServiceImpl implements UserService {

    private final Jophiel jophiel;
    private final UserDao userDao;

    public UserServiceImpl(Jophiel jophiel, UserDao userDao) {
        this.jophiel = jophiel;
        this.userDao = userDao;
    }

    @Override
    public void upsertUser(String userJid, String accessToken, String idToken, String refreshToken, long expireTime) {
        if (userDao.existsByUserJid(userJid)) {
            UserModel userModel = userDao.findByUserJid(userJid);

            userModel.accessToken = accessToken;
            userModel.refreshToken = refreshToken;
            userModel.idToken = idToken;
            userModel.expirationTime = expireTime;

            userDao.edit(userModel, "guest", IdentityUtils.getIpAddress());
        } else {
            UserModel userModel = new UserModel();
            userModel.userJid = userJid;
            userModel.roles = StringUtils.join(SandalphonUtils.getDefaultRoles(), ",");

            userModel.accessToken = accessToken;
            userModel.refreshToken = refreshToken;
            userModel.idToken = idToken;
            userModel.expirationTime = expireTime;

            userDao.persist(userModel, "guest", IdentityUtils.getIpAddress());
        }
    }


    @Override
    public boolean existsByUserJid(String userJid) {
        return userDao.existsByUserJid(userJid);
    }

    @Override
    public User findUserById(long userId) {
        UserModel userRoleModel = userDao.findById(userId);
        return createUserRoleFromModel(userRoleModel);
    }

    @Override
    public User findUserByUserJid(String userJid) {
        UserModel userRoleModel = userDao.findByUserJid(userJid);
        return createUserRoleFromModel(userRoleModel);
    }

    @Override
    public void createUser(String userJid, List<String> roles) {
        UserModel userRoleModel = new UserModel();
        userRoleModel.userJid = userJid;
        userRoleModel.roles = StringUtils.join(roles, ",");

        userDao.persist(userRoleModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public void updateUser(long userRoleId, List<String> roles) {
        UserModel userRoleModel = userDao.findById(userRoleId);
        userRoleModel.roles = StringUtils.join(roles, ",");

        userDao.edit(userRoleModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public void deleteUser(long userRoleId) {
        UserModel userRoleModel = userDao.findById(userRoleId);
        userDao.remove(userRoleModel);
    }

    @Override
    public Page<User> pageUsers(long pageIndex, long pageSize, String orderBy, String orderDir, String filterString) {
        long totalPages = userDao.countByFilters(filterString, ImmutableMap.of(), ImmutableMap.of());
        List<UserModel> userRoleModels = userDao.findSortedByFilters(orderBy, orderDir, filterString, ImmutableMap.of(), ImmutableMap.of(), pageIndex * pageSize, pageSize);

        List<User> userRoles = Lists.transform(userRoleModels, m -> createUserRoleFromModel(m));

        return new Page<>(userRoles, totalPages, pageIndex, pageSize);
    }

    @Override
    public void upsertUserFromJophielUserJid(String userJid) {
        try {
            org.iatoki.judgels.jophiel.commons.User user = jophiel.getUserByUserJid(userJid);

            if (!userDao.existsByUserJid(userJid))
                createUser(user.getJid(), SandalphonUtils.getDefaultRoles());

            JidCacheService.getInstance().putDisplayName(user.getJid(), JudgelsUtils.getUserDisplayName(user.getUsername(), user.getName()), IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
        } catch (IOException e) {
            // do nothing
        }
    }

    @Override
    public UserTokens getUserTokensByUserJid(String userJid) {
        UserModel userModel = userDao.findByUserJid(userJid);

        return createUserTokensFromUserModel(userModel);
    }

    private UserTokens createUserTokensFromUserModel(UserModel userModel) {
        return new UserTokens(userModel.userJid, userModel.accessToken, userModel.refreshToken, userModel.idToken, userModel.expirationTime);
    }

    private User createUserRoleFromModel(UserModel userRoleModel) {
        return new User(userRoleModel.id, userRoleModel.userJid, Arrays.asList(userRoleModel.roles.split(",")));
    }
}
