package com.zupcat.dao;

import com.zupcat.model.DatastoreEntity;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This class is a wrapper for Datastore operations. Supports most of the DatastoreService and DatastoreAsyncService operations adding features such as:
 * <p/>
 * - Entity to "DatastoreEntity" convertions
 * - caching usage
 * - retrying algorithms
 * - performance logging
 * - remote client massive and parallel data access
 * <p/>
 * Every X_DataStoreEntity should have its X_DAO implementation. See tests for examples
 */
public interface IDAO<P extends DatastoreEntity> extends Serializable {

    // Querying methods =====
    P findById(final String id);

    FutureEntity<P> findByIdAsync(final String id);

    Map<String, P> findUniqueIdMultiple(final Collection<String> ids);

    List<P> getAll();

    // Updating methods =====
    void updateOrPersist(final P persistentObject);

    void updateOrPersistAsync(final P persistentObject);

    void remove(final String id);

    void removeAsync(final String id);

    void remove(final Collection<String> ids);
}
