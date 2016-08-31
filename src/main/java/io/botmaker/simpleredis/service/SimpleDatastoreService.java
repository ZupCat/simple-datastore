package io.botmaker.simpleredis.service;

import io.botmaker.simpleredis.dao.DAO;

/**
 * Entry point for service access
 */
public interface SimpleDatastoreService {

    void setDatastoreCallsLogging(final boolean activate);

    boolean isDatastoreCallsLoggingActivated();

    boolean isProductionEnvironment();

    void registerDAO(final DAO dao);

    <T extends DAO> T getDAO(final Class<T> daoClass);

    DAO getDAO(final String entityName);

    // Redis methods
    void configRedisServer(final String appId, final String redisHost, final int redisPost, final boolean isProductionEnvironment);

    void configRedisServer(final String appId, final String redisHost, final int redisPort, final boolean isProductionEnvironment, final String redisAuthPassword);

    RedisServer getRedisServer();
}
