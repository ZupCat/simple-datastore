package io.botmaker.simpleredis.dao;

import io.botmaker.simpleredis.model.DataObject;
import io.botmaker.simpleredis.model.RedisEntity;
import io.botmaker.simpleredis.service.SimpleDatastoreService;
import io.botmaker.simpleredis.service.SimpleDatastoreServiceFactory;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class is a wrapper for Datastore operations. Supports most of the DatastoreService and DatastoreAsyncService operations adding features such as:
 * <p>
 * - Entity to "RedisEntity" convertions
 * - caching usage
 * - retrying algorithms
 * - performance logging
 * - remote client massive and parallel data access
 * <p>
 * Every X_RedisEntity should have its X_DAO implementation. See tests for examples
 */
public class DAO<P extends RedisEntity> implements Serializable, IDAO<P> {

    //    protected static final Logger log = Logger.getLogger(DAO.class.getName());
    private static final long serialVersionUID = 471847964351314234L;
    private static final RetryingHandler RETRYING_HANDLER = new RetryingHandler();
    //    protected final P sample;
    protected final Class<? extends P> beanClass;
    protected final P sample;
    private final DAOCustomByIdCache<P> customByIdCache;
    private String entityName;

    public DAO(final Class<? extends P> beanClass) {
        this(beanClass, null);
    }

    public DAO(final Class<? extends P> beanClass, final DAOCustomByIdCache<P> customByIdCache) {
        this.beanClass = beanClass;
        this.sample = buildPersistentObjectInstance();
        this.entityName = RedisEntity.getEntityName(beanClass);
        this.customByIdCache = customByIdCache;

        if (customByIdCache != null) {
            customByIdCache.setDAO(this);
        }
    }

    public void save(final P persistentObject) {
        save(persistentObject, false);
    }

    public void save(final P persistentObject, final boolean avoidUsingCustomCache) {
        if (!avoidUsingCustomCache && customByIdCache != null && customByIdCache.alternativeSave(persistentObject))
            return;

        prepareForUpdateOrPersist(persistentObject);

        getRetryingHandler().tryDSPut(this, persistentObject);
    }

    private void prepareForUpdateOrPersist(final P persistentObject) {
        persistentObject.setModified();
    }

    public List<P> getAll() {
        return new ArrayList<>(getRetryingHandler().tryDSGetAll(this));
    }

    public P findById(final String id) {
        if (id == null)
            return null;

        final P cached = customByIdCache == null ? null : customByIdCache.get(id);
        if (cached != null)
            return cached;

        final P result = RETRYING_HANDLER.tryDSGet(id, this);
        if (customByIdCache != null)
            customByIdCache.put(id, result);
        return result;

    }

    protected DAOCustomByIdCache<P> getCustomByIdCache() {
        return customByIdCache;
    }

    public Map<String, P> findUniqueIdMultiple(final Collection<String> ids) {
        return new HashMap<>(getRetryingHandler().tryDSGetMultiple(ids, this));
    }

    public void massiveUpload(final Collection<P> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        getRetryingHandler().tryDSPutMultiple(this, list);
    }

    public String getDataRedixPrefix() {
        final SimpleDatastoreService simpleDatastoreService = SimpleDatastoreServiceFactory.getSimpleDatastoreService();

        return getRetryingHandler().buildKey(entityName, "", sample.usesAppIdPrefix(), simpleDatastoreService.isProductionEnvironment(),
                simpleDatastoreService.getRedisServer());
    }

    public void remove(final P persistentObject) {

        final String id = persistentObject.getId();
        getRetryingHandler().tryDSRemove(id, this);
        if (customByIdCache != null)
            customByIdCache.remove(id);
    }

    public void remove(final Collection<P> persistentObjects) {

        final Set<String> ids = persistentObjects.stream().map(RedisEntity::getId).collect(Collectors.toSet());
        getRetryingHandler().tryDSRemove(ids, this);
        if (customByIdCache != null)
            ids.forEach(customByIdCache::remove);
    }

    protected Predicate<RedisEntity> getFilterPredicate() {
        // for Override
        return null;
    }

    public RetryingHandler getRetryingHandler() {
        return RETRYING_HANDLER;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(final String value) {
        entityName = value;
    }

    @Override
    public P buildPersistentObjectInstanceFromPersistedStringData(final String persistedStringData) {
        final P result = buildPersistentObjectInstance();
        result.isPersisted(true);
        result.getDataObject().mergeWith(new DataObject(persistedStringData));

        return result;
    }

    @Override
    public P buildPersistentObjectInstance() {
        try {
            return beanClass.newInstance();
        } catch (final Exception _exception) {
            throw new RuntimeException("Problems instantiating class [" + beanClass.getName() + "]. Maybe missing empty constructor?: " + _exception.getMessage(), _exception);
        }
    }

    public P getSample() {
        return sample;
    }

    @Override
    public P findUniqueByIndexableProperty(final String optBusinessIdForCache, final String propertyName, final String id) {
        if (id == null || id.trim().length() == 0) {
            return null;
        }

        final List<P> cached = customByIdCache == null ? null : customByIdCache.getByParams(propertyName, id);
        if (cached != null && !cached.isEmpty())
            return cached.get(0);


        final List<P> list = getRetryingHandler().tryDSGetByIndexableProperty(propertyName, id, this);

        if (customByIdCache != null)
            customByIdCache.putByParams(list, propertyName, id);

        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<P> findMultipleByIndexableProperty(final String optBusinessIdForCache, final String propertyName, final String id) {
        if (id == null || id.trim().length() == 0) {
            return Collections.emptyList();
        }

        final List<P> cached = customByIdCache == null ? Collections.emptyList() : customByIdCache.getByParams(propertyName, id);
        if (cached != null && !cached.isEmpty())
            return new ArrayList<>(cached);

        final List<P> result = getRetryingHandler().tryDSGetByIndexableProperty(propertyName, id, this);

        if (customByIdCache != null)
            customByIdCache.putByParams(result, propertyName, id);

        return new ArrayList<>(result);
    }

    @Override
    public List<P> findMultipleLastOccurrencesByIndexableProperty(final String optBusinessIdForCache, final String propertyName, final int ocurrences, final String id) {
        if (id == null || id.trim().length() == 0) {
            return Collections.emptyList();
        }
        return new ArrayList<>(getRetryingHandler().tryDSGetLastOccurrencesByIndexableProperty(propertyName, id, ocurrences, this));
    }

    @Override
    public List<P> findMultipleSortedFromToByIndexableProperty(final String optBusinessIdForCache, final String propertyName, final String id, final int from, final int to) {
        if (id == null || id.trim().length() == 0) {
            return Collections.emptyList();
        }
        return new ArrayList<>(getRetryingHandler().tryDSGetFromToByIndexableProperty(propertyName, id, from, to, this));
    }

    @Override
    public List<P> findMultipleIntersectionOfIndexableProperty(final String optBusinessIdForCache, final Map<String, String> propertyNameAndValueMap) {
        if (propertyNameAndValueMap == null || propertyNameAndValueMap.size() == 0) {
            return Collections.emptyList();
        }

        final List<P> cached = customByIdCache == null ? null : customByIdCache.getByParams(propertyNameAndValueMap);
        if (cached != null && !cached.isEmpty())
            return new ArrayList<>(cached);

        final List<P> result = getRetryingHandler().tryDSGetIntersectionOfIndexableProperties(this, propertyNameAndValueMap);

        if (customByIdCache != null)
            customByIdCache.putByParams(result, propertyNameAndValueMap);

        return new ArrayList<>(result);
    }

    @Override
    public List<P> findMultipleUnionOfIndexableProperty(final List<Pair<String, String>> propertyNameAndValuePair) {
        if (propertyNameAndValuePair == null || propertyNameAndValuePair.size() == 0) {
            return Collections.emptyList();
        }
        return new ArrayList<>(getRetryingHandler().tryDSGetUnionOfIndexableProperties(this, propertyNameAndValuePair));
    }
}
