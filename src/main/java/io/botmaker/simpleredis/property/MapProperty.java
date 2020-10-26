package io.botmaker.simpleredis.property;

import io.botmaker.simpleredis.model.DataObject;
import io.botmaker.simpleredis.model.RedisEntity;
import io.botmaker.simpleredis.model.config.PropertyMeta;
import org.json.JSONObject;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Work as Map<String, Object>
 */
public final class MapProperty<V> extends PropertyMeta<Map<String, V>> implements Serializable, Map<String, V> {

    private static final long serialVersionUID = 6181606486836703354L;

    private static final Object LOCK_OBJECT_ARRAY = new Object();
    private static Field _arrayField;
    private final Class<? extends DataObject> valueClass;

    public MapProperty(final RedisEntity owner, final Class<? extends DataObject> _valueClass) {
        super(owner);
        valueClass = _valueClass;
    }

    public Class<? extends DataObject> getValueClass() {
        return valueClass;
    }

    @Override
    protected Map<String, V> getValueImpl(final DataObject dataObject) {
        final Map result = getMapImpl(dataObject);

        if (!result.isEmpty() && valueClass != null) {
            final boolean isSameType = valueClass.isInstance(result.values().iterator().next());

            if (!isSameType) {
                final Map<String, DataObject> tempMap = new HashMap<>(result.size());

                for (final Object entryObject : result.entrySet()) {
                    final Entry<String, JSONObject> entry = (Entry<String, JSONObject>) entryObject;
                    final DataObject convertedItem;

                    try {
                        convertedItem = valueClass.newInstance();
                    } catch (final Exception _exception) {
                        throw new RuntimeException("Could not instantiate object of class [" + valueClass.getName() + "]. Maybe missing empty constructor?: " + _exception.getMessage(), _exception);
                    }

                    convertedItem.mergeWith(entry.getValue());
                    tempMap.put(entry.getKey(), convertedItem);
                }

                result.clear();
                result.putAll(tempMap);
            }
        }
        return (Map<String, V>) result;
    }

    @Override
    public void setFromStringValue(final String stringValue, final boolean forceAudit) {
        try {
            final StringTokenizer stringTokenizer = new StringTokenizer(stringValue, ";");
            final Map map = new HashMap<>(stringTokenizer.countTokens());
            while (stringTokenizer.hasMoreElements()) {
                final String[] entry = stringTokenizer.nextElement().toString().split(",");
                map.put(entry[0].trim(), entry[1].trim());
            }
            set(map, forceAudit);
        } catch (final Exception e) {
            throw new UnsupportedOperationException("MapProperty only support set from string for string's key & value", e);
        }
    }

    @Override
    protected void setValueImpl(final Map<String, V> value, final DataObject dataObject) {
        if (value == null || value.isEmpty()) {
            dataObject.remove(name);
        } else {
            dataObject.put(name, value);
        }
    }


    private Map<String, V> getMap() {
        return getValueImpl(getOwner().getDataObject());
    }

    private Map<String, V> getMapImpl(final DataObject dataObject) {
        final JSONObject jsonObject;

        if (dataObject.has(name)) {
            jsonObject = dataObject.getJSONObject(name);
        } else {
            jsonObject = new JSONObject();
            dataObject.put(name, jsonObject);
        }
        return getInternalMapFromJSONObject(jsonObject);
    }


    private Map<String, V> getInternalMapFromJSONObject(final JSONObject jsonObject) {
        try {
            return (Map<String, V>) getInternapArrayField().get(jsonObject);
        } catch (final Exception _exception) {
            throw new RuntimeException("Problems when getting JSONObject internal map field using reflection for array [" + jsonObject + ": " + _exception.getMessage(), _exception);
        }
    }

    private static Field getInternapArrayField() {
        if (_arrayField == null) {
            synchronized (LOCK_OBJECT_ARRAY) {
                if (_arrayField == null) {
                    try {
                        final Field arrayField = JSONObject.class.getDeclaredField("map");

                        arrayField.setAccessible(true);

                        _arrayField = arrayField;

                    } catch (final Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return _arrayField;
    }

    // Reading operations
    public int size() {
        return getMap().size();
    }

    public boolean isEmpty() {
        return getMap().isEmpty();
    }

    public boolean containsKey(final Object key) {
        return getMap().containsKey(key);
    }

    public boolean containsValue(final Object value) {
        return getMap().containsValue(value);
    }

    public V get(final Object key) {
        return getMap().get(key);
    }

    public Set<String> keySet() {
        return getMap().keySet();
    }

    public Collection<V> values() {
        return getMap().values();
    }

    public Set<Entry<String, V>> entrySet() {
        return getMap().entrySet();
    }


    // Modification Operations
    public V put(final String key, final V value) {
        return getMap().put(key, value);
    }

    public V remove(final Object key) {
        return getMap().remove(key);
    }

    public void putAll(final Map<? extends String, ? extends V> m) {
        getMap().putAll(m);
    }

    public void clear() {
        getMap().clear();
    }
}
