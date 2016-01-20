package com.zupcat.dao;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.datastore.DatastoreV1;
import com.google.api.services.datastore.client.*;
import com.google.appengine.api.datastore.*;
import com.google.protobuf.ByteString;
import com.zupcat.exception.NoMoreRetriesException;
import com.zupcat.service.SimpleDatastoreService;
import com.zupcat.service.SimpleDatastoreServiceFactory;
import com.zupcat.util.NoFuture;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Proxy for all Datastore ops. It retries the call if it has problems
 *
 * @see "http://code.google.com/appengine/articles/handling_datastore_errors.html"
 * @see "http://code.google.com/appengine/docs/java/datastore/transactions.html"
 */
public final class RetryingHandler implements Serializable {

    private static final long serialVersionUID = 472842924253314234L;
    private static final Logger log = Logger.getLogger(RetryingHandler.class.getName());

    private static final int MAX_RETRIES = 6;
    private static final int WAIT_MS = 800;

    private Datastore remoteDatastore;


    public static void sleep(final int millis) {
        try {
            Thread.sleep(millis);

        } catch (final InterruptedException ie) {
            // nothing to do
        }
    }

    public Entity tryExecuteQueryWithSingleResult(final Query query) {
        final Entity[] result = new Entity[1];
        result[0] = null;

        tryClosure(new Closure() {
            public void execute(final DatastoreService datastore, final Object[] results, final boolean loggingActivated) {
                if (loggingActivated) {
                    log.log(Level.SEVERE, "PERF - tryExecuteQueryWithSingleResult", new Exception());
                }

                final PreparedQuery preparedQuery = datastore.prepare(query);
                result[0] = preparedQuery.asSingleEntity();
            }
        }, result);

        return result[0];
    }

    public QueryResultList<Entity> tryExecuteQuery(final Query query) {
        return tryExecuteQuery(query, FetchOptions.Builder.withDefaults());
    }

    public QueryResultList<Entity> tryExecuteQuery(final Query query, final FetchOptions fetchOptions) {
        final QueryResultList<Entity>[] result = new QueryResultList[1];
        result[0] = null;

        tryClosure(new Closure() {
            public void execute(final DatastoreService datastore, final Object[] results, final boolean loggingActivated) {
                if (loggingActivated) {
                    log.log(Level.SEVERE, "PERF - tryExecuteQuery", new Exception());
                }

                final PreparedQuery preparedQuery = datastore.prepare(query);
                result[0] = preparedQuery.asQueryResultList(fetchOptions);
            }
        }, result);

        return result[0];
    }

    public void tryDSRemove(final Collection<Key> entityKeys) {
        tryClosure(new Closure() {
            public void execute(final DatastoreService datastore, final Object[] results, final boolean loggingActivated) {

                if (loggingActivated) {
                    log.log(Level.SEVERE, "PERF - tryDSRemoveMultiple", new Exception());
                }

                datastore.delete(entityKeys);
            }
        }, null);
    }

    public Entity tryDSGet(final Key entityKey) {
        final Entity[] result = new Entity[1];
        result[0] = null;

        tryClosure(new Closure() {
            public void execute(final DatastoreService datastore, final Object[] results, final boolean loggingActivated) {
                if (loggingActivated) {
                    log.log(Level.SEVERE, "PERF - tryDSGet", new Exception());
                }

                Entity resultEntity = null;

                try {
                    if (remoteDatastore != null) {
                        // Set the entity key with only one `path_element`: no parent.
                        final DatastoreV1.Key.Builder key = DatastoreV1.Key.newBuilder()
                                .addPathElement(
                                        DatastoreV1.Key.PathElement.newBuilder()
                                                .setKind(entityKey.getKind())
                                                .setName(entityKey.getName())
                                );

                        // Execute the RPC and get the response.
                        final DatastoreV1.LookupResponse lookupResponse = remoteDatastore.lookup(
                                DatastoreV1.LookupRequest.newBuilder().addKey(key).build()
                        );

                        if (lookupResponse.getFoundCount() > 0) {
                            final Map<String, DatastoreV1.Value> properties = DatastoreHelper.getPropertyMap(lookupResponse.getFound(0).getEntity());
                            resultEntity = new Entity(entityKey);

                            for (final Map.Entry<String, DatastoreV1.Value> entry : properties.entrySet()) {
                                final DatastoreV1.Value value = entry.getValue();
                                Object propertyValue;

                                if (value.hasIntegerValue()) {
                                    propertyValue = value.getIntegerValue();
                                } else if (value.hasStringValue()) {
                                    propertyValue = value.getStringValue();
                                } else if (value.hasBooleanValue()) {
                                    propertyValue = value.getBooleanValue();
                                } else if (value.hasBlobValue()) {
                                    propertyValue = value.getBlobValue();
                                } else {
                                    throw new UnsupportedOperationException("Unsupported value: " + value);
                                }
                                resultEntity.setProperty(entry.getKey(), propertyValue);
                            }
                        }
                    } else {
                        resultEntity = datastore.get(entityKey);
                    }
                } catch (final EntityNotFoundException _entityNotFoundException) {
                    // nothing to do
                } catch (final DatastoreException _datastoreException) {
                    throw new RuntimeException(_datastoreException.getMessage(), _datastoreException);
                }

                results[0] = resultEntity;
            }
        }, result);

        return result[0];
    }

    public Future<Entity> tryDSGetAsync(final Key entityKey) {
        return tryClosureAsync(new AsyncClosure<Entity>() {
            public Future<Entity> execute(final AsyncDatastoreService datastore, final boolean loggingActivated) throws ExecutionException, InterruptedException {
                if (loggingActivated) {
                    log.log(Level.SEVERE, "PERF - tryDSGetAsync", new Exception());
                }

                if (remoteDatastore != null) {
                    return new NoFuture<>(tryDSGet(entityKey));
                } else {
                    return datastore.get(entityKey);
                }
            }
        });
    }

    public void tryDSRemove(final Key entityKey) {
        tryClosure(new Closure() {
            public void execute(final DatastoreService datastore, final Object[] results, final boolean loggingActivated) {
                if (loggingActivated) {
                    log.log(Level.SEVERE, "PERF - tryDSRemove", new Exception());
                }

                datastore.delete(entityKey);
            }
        }, null);
    }

    public void tryDSRemoveAsync(final Key key) {
        tryClosureAsync(new AsyncClosure<Void>() {

            public Future<Void> execute(final AsyncDatastoreService datastore, final boolean loggingActivated) throws ExecutionException, InterruptedException {
                if (loggingActivated) {
                    log.log(Level.SEVERE, "PERF - tryDSRemoveAsync", new Exception());
                }

                return datastore.delete(key);
            }
        });
    }

    public void tryDSPutMultipleAsync(final Iterable<Entity> entities) {
        tryClosureAsync(new AsyncClosure<List<Key>>() {

            public Future<List<Key>> execute(final AsyncDatastoreService datastore, final boolean loggingActivated) throws ExecutionException, InterruptedException {
                if (loggingActivated) {
                    log.log(Level.SEVERE, "PERF - tryDSPutMultipleAsync", new Exception());
                }

                if (remoteDatastore != null) {
                    throw new UnsupportedOperationException("tryDSPutMultipleAsync is not currently supported on remote Datastore");
                }

                final Future<List<Key>> listFuture = datastore.put(entities);

                listFuture.get();

                return listFuture;
            }
        });
    }


//    public void tryDSPutMultiple(final Iterable<Entity> entities) {
//        tryClosure(new Closure() {
//            public void execute(final DatastoreService datastore, final Object[] results, final boolean loggingActivated) {
//                if (loggingActivated) {
//                    log.log(Level.SEVERE, "PERF - tryDSPutMultiple", new Exception());
//                }
//                datastore.put(entities);
//            }
//        }, null);
//    }

    public Map<Key, Entity> tryDSGetMultiple(final Collection<Key> keys) {
        final Map<Key, Entity> result = new HashMap<>();

        if (keys != null && !keys.isEmpty()) {
            tryClosure(new Closure() {

                @Override
                public void execute(final DatastoreService datastore, final Object[] results, final boolean loggingActivated) {
                    if (loggingActivated) {
                        log.log(Level.SEVERE, "PERF - tryDSGetMultiple", new Exception());
                    }

                    if (remoteDatastore != null) {
                        throw new UnsupportedOperationException("tryDSGetMultiple is not currently supported on remote DataStore");
                    }

                    result.putAll(datastore.get(keys));
                }
            }, null);
        }
        return result;
    }

    public void tryDSPut(final Entity entity) {
        tryClosure(new Closure() {
            public void execute(final DatastoreService datastore, final Object[] results, final boolean loggingActivated) {
                if (loggingActivated) {
                    log.log(Level.SEVERE, "PERF - tryDSPut", new Exception());
                }

                if (remoteDatastore != null) {
                    try {
                        final DatastoreV1.Entity.Builder entityBuilder = DatastoreV1.Entity.newBuilder();

                        // Set the entity key.
                        final Key entityKey = entity.getKey();
                        final DatastoreV1.Key.Builder key = DatastoreV1.Key.newBuilder()
                                .addPathElement(
                                        DatastoreV1.Key.PathElement.newBuilder()
                                                .setKind(entityKey.getKind())
                                                .setName(entityKey.getName())
                                );

                        entityBuilder.setKey(key);

                        // properties
                        for (final Map.Entry<String, Object> entry : entity.getProperties().entrySet()) {
                            final DatastoreV1.Value.Builder valueBuilder = DatastoreV1.Value.newBuilder();
                            final String propertyName = entry.getKey();
                            final Object propertyValue = entry.getValue();

                            valueBuilder.setIndexed(!entity.isUnindexedProperty(propertyName));

                            if (propertyValue instanceof Long) {
                                valueBuilder.setIntegerValue((long) propertyValue);
                            } else if (propertyValue instanceof Integer) {
                                valueBuilder.setIntegerValue((int) propertyValue);
                            } else if (propertyValue instanceof String) {
                                valueBuilder.setStringValue(propertyValue.toString());
                            } else if (propertyValue instanceof Boolean) {
                                valueBuilder.setBooleanValue((boolean) propertyValue);
                            } else if (propertyValue instanceof Blob) {
                                valueBuilder.setBlobValue(ByteString.copyFrom(((Blob) propertyValue).getBytes()));
                            } else {
                                throw new UnsupportedOperationException("Unsupported value: " + propertyValue);
                            }

                            entityBuilder.addProperty(
                                    DatastoreV1.Property.newBuilder()
                                            .setName(propertyName)
                                            .setValue(valueBuilder.build())
                            );
                        }

                        // Build the entity.
                        final DatastoreV1.Entity remoteEntity = entityBuilder.build();

                        // Execute the RPC synchronously.
                        final DatastoreV1.BeginTransactionResponse beginTransactionResponse =
                                remoteDatastore.beginTransaction(DatastoreV1.BeginTransactionRequest.newBuilder().build());

                        // Create an RPC request to commit the transaction.
                        final DatastoreV1.CommitRequest.Builder commitRequestBuilder =
                                DatastoreV1.CommitRequest.newBuilder()
                                        .setTransaction(beginTransactionResponse.getTransaction());

                        // Insert the entity in the commit request mutation.
                        commitRequestBuilder.getMutationBuilder().addInsert(remoteEntity);

                        // Execute the Commit RPC synchronously and ignore the response.
                        // Apply the insert mutation if the entity was not found and close
                        // the transaction.
                        remoteDatastore.commit(commitRequestBuilder.build());
                    } catch (final DatastoreException datastoreException) {
                        throw new RuntimeException(datastoreException.getMessage(), datastoreException);
                    }
                } else {
                    datastore.put(entity);
                }
            }
        }, null);
    }

    public void tryDSPutAsync(final Entity entity) {
        tryClosureAsync(new AsyncClosure<Key>() {
            public Future<Key> execute(final AsyncDatastoreService datastore, final boolean loggingActivated) throws ExecutionException, InterruptedException {
                if (loggingActivated) {
                    log.log(Level.SEVERE, "PERF - tryDSPutAsync", new Exception());
                }

                if (remoteDatastore != null) {
                    tryDSPut(entity);
                    return new NoFuture<>(entity.getKey());
                } else {
                    return datastore.put(entity);
                }
            }
        });
    }

    private void tryClosure(final Closure closure, final Object[] results) {
        final ValuesContainer values = new ValuesContainer();
        final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        final SimpleDatastoreService simpleDatastoreService = SimpleDatastoreServiceFactory.getSimpleDatastoreService();
        final boolean loggingActivated = simpleDatastoreService.isDatastoreCallsLoggingActivated();

        checkConfigProtoBuf(simpleDatastoreService);

        while (true) {
            try {
                closure.execute(datastore, results, loggingActivated);
                break;
            } catch (final DatastoreTimeoutException dte) {
                handleError(values, dte, true);
            } catch (final Exception exception) {
                handleError(values, exception, false);
            }
        }
    }

    private void checkConfigProtoBuf(final SimpleDatastoreService simpleDatastoreService) {
        if (simpleDatastoreService.isProtoBufMode() && remoteDatastore == null) {
            try {
                final GoogleCredential.Builder credentialBuilder = new GoogleCredential.Builder();
                credentialBuilder.setTransport(GoogleNetHttpTransport.newTrustedTransport());
                credentialBuilder.setJsonFactory(new JacksonFactory());
                credentialBuilder.setServiceAccountId(simpleDatastoreService.getDatastoreServiceAccountEmail());
                credentialBuilder.setServiceAccountScopes(DatastoreOptions.SCOPES);
                credentialBuilder.setServiceAccountPrivateKeyFromP12File(new File(simpleDatastoreService.getDatastorePrivateKeyP12FileLocation()));

                final DatastoreOptions.Builder datastoreBuilder = new DatastoreOptions.Builder();
                datastoreBuilder.dataset(simpleDatastoreService.getDataSetId());
                datastoreBuilder.credential(credentialBuilder.build());

                remoteDatastore = DatastoreFactory.get().create(datastoreBuilder.build());

            } catch (final Exception e) {
                log.log(Level.SEVERE, "Problems trying to getting a connection to remote DataStore using protobuf mode. DataSetId [" + simpleDatastoreService.getDataSetId() +
                        "], ServiceAccountEmail [" + simpleDatastoreService.getDatastoreServiceAccountEmail() +
                        "], PrivateKeyP12FileLocation [" + simpleDatastoreService.getDatastorePrivateKeyP12FileLocation() +
                        "]: " + e.getMessage(), e);

            }
        }
    }


    private <T> Future<T> tryClosureAsync(final AsyncClosure<T> closure) {
        final ValuesContainer values = new ValuesContainer();
        final AsyncDatastoreService datastore = DatastoreServiceFactory.getAsyncDatastoreService();
        Future<T> result;
        final SimpleDatastoreService simpleDatastoreService = SimpleDatastoreServiceFactory.getSimpleDatastoreService();

        final boolean loggingActivated = simpleDatastoreService.isDatastoreCallsLoggingActivated();
        checkConfigProtoBuf(simpleDatastoreService);

        while (true) {
            try {
                result = closure.execute(datastore, loggingActivated);
                break;
            } catch (final InterruptedException | DatastoreTimeoutException ie) {
                handleError(values, ie, true);
            } catch (final Exception exception) {
                handleError(values, exception, false);
            }
        }
        return result;
    }

    private void handleError(final ValuesContainer values, final Exception exception, final boolean isTimeoutException) {
        values.retry = values.retry - 1;

        if (values.retry == 0) {
            log.log(Level.SEVERE, "PERF - No more tries for datastore access: " + exception.getMessage(), exception);
            throw new NoMoreRetriesException(exception);
        }

        sleep(values.retryWait);

        if (isTimeoutException) {
            values.retryWait = values.retryWait * 3;
        }
    }


    private interface Closure {

        void execute(final DatastoreService datastore, final Object[] results, final boolean loggingActivated);
    }


    private interface AsyncClosure<T> {

        Future<T> execute(final AsyncDatastoreService datastore, final boolean loggingActivated) throws ExecutionException, InterruptedException;
    }


    public static final class ValuesContainer implements Serializable {

        private static final long serialVersionUID = 472142124257311224L;

        public int retry = MAX_RETRIES;
        public int retryWait = WAIT_MS;
    }
}
