package com.zupcat.dao;

import com.google.appengine.api.datastore.*;
import com.zupcat.exception.NoMoreRetriesException;
import com.zupcat.service.SimpleDatastoreService;
import com.zupcat.service.SimpleDatastoreServiceFactory;

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
                    resultEntity = datastore.get(entityKey);
                } catch (final EntityNotFoundException _entityNotFoundException) {
                    // nothing to do
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

                return datastore.get(entityKey);
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

                datastore.put(entity);
            }
        }, null);
    }

    public void tryDSPutAsync(final Entity entity) {
        tryClosureAsync(new AsyncClosure<Key>() {
            public Future<Key> execute(final AsyncDatastoreService datastore, final boolean loggingActivated) throws ExecutionException, InterruptedException {
                if (loggingActivated) {
                    log.log(Level.SEVERE, "PERF - tryDSPutAsync", new Exception());
                }

                return datastore.put(entity);
            }
        });
    }

    private void tryClosure(final Closure closure, final Object[] results) {
        final ValuesContainer values = new ValuesContainer();
        final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        final SimpleDatastoreService simpleDatastoreService = SimpleDatastoreServiceFactory.getSimpleDatastoreService();
        final boolean loggingActivated = simpleDatastoreService.isDatastoreCallsLoggingActivated();

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

    private <T> Future<T> tryClosureAsync(final AsyncClosure<T> closure) {
        final ValuesContainer values = new ValuesContainer();
        final AsyncDatastoreService datastore = DatastoreServiceFactory.getAsyncDatastoreService();
        Future<T> result;
        final SimpleDatastoreService simpleDatastoreService = SimpleDatastoreServiceFactory.getSimpleDatastoreService();

        final boolean loggingActivated = simpleDatastoreService.isDatastoreCallsLoggingActivated();

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

//    private void checkConfigProtoBuf(final SimpleDatastoreService simpleDatastoreService) {
//        if (simpleDatastoreService.isProtoBufMode() && remoteDatastore == null) {
//            try {
//                final GoogleCredential.Builder credentialBuilder = new GoogleCredential.Builder();
//                credentialBuilder.setTransport(GoogleNetHttpTransport.newTrustedTransport());
//                credentialBuilder.setJsonFactory(new JacksonFactory());
//                credentialBuilder.setServiceAccountId(simpleDatastoreService.getDatastoreServiceAccountEmail());
//                credentialBuilder.setServiceAccountScopes(DatastoreOptions.SCOPES);
//                credentialBuilder.setServiceAccountPrivateKeyFromP12File(new File(simpleDatastoreService.getDatastorePrivateKeyP12FileLocation()));
//
//                final DatastoreOptions.Builder datastoreBuilder = new DatastoreOptions.Builder();
//                datastoreBuilder.dataset(simpleDatastoreService.getDataSetId());
//                datastoreBuilder.credential(credentialBuilder.build());
//
//                remoteDatastore = DatastoreFactory.get().create(datastoreBuilder.build());
//
//            } catch (final Exception e) {
//                log.log(Level.SEVERE, "Problems trying to getting a connection to remote DataStore using protobuf mode. DataSetId [" + simpleDatastoreService.getDataSetId() +
//                        "], ServiceAccountEmail [" + simpleDatastoreService.getDatastoreServiceAccountEmail() +
//                        "], PrivateKeyP12FileLocation [" + simpleDatastoreService.getDatastorePrivateKeyP12FileLocation() +
//                        "]: " + e.getMessage(), e);
//
//            }
//        }
//    }

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
