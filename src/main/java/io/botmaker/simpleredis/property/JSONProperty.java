package io.botmaker.simpleredis.property;

import io.botmaker.simpleredis.model.DataObject;
import io.botmaker.simpleredis.model.RedisEntity;
import io.botmaker.simpleredis.model.config.PropertyMeta;
import org.json.JSONObject;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public final class JSONProperty extends PropertyMeta<Map<String, Object>> implements Serializable {

    private static final long serialVersionUID = 6181606486836703324L;
    private static final Object LOCK_OBJECT_ARRAY = new Object();
    private static Field _arrayField;
//    private static final JsonFactory jsonFactory = new JsonFactory();
//    private static final ObjectMapper mapper = new ObjectMapper(jsonFactory);

//    @JsonIgnore
//    private final Function<ObjectMapper, TypeReference> typeReference;

    public JSONProperty(final RedisEntity owner) {
        super(owner);
//        this.typeReference = mapper -> new TypeReference<Map<String, Object>>() {
//        };
    }

    @Override
    protected Map<String, Object> getValueImpl(final DataObject dataObject) {
        final JSONObject jsonObject = dataObject.optJSONObject(name);
        if (jsonObject == null)
            return null;

        return getInternalMapFromJSONObject(jsonObject);

//        try {
//            return mapper.readValue(jsonFactory.createParser(new StringReader(string)), typeReference.apply(mapper));
//        } catch (final IOException e) {
//            throw new RuntimeException(e);
//        }
    }

    @Override
    public void setFromStringValue(final String stringValue, final boolean forceAudit) {
        throw new UnsupportedOperationException("JSONProperty: setFromStringValue method is not implemented");
    }

    @Override
    protected void setValueImpl(final Map<String, Object> value, final DataObject dataObject) {
        if (value == null || value.isEmpty()) {
            dataObject.remove(name);
        } else {
            dataObject.put(name, value);
        }

//        try {
//            final StringWriter stringWriter = new StringWriter(500);
//            mapper.writeValue(stringWriter, value);
//            stringWriter.close();
//            dataObject.put(name, stringWriter.toString());
//        } catch (final IOException e) {
//            throw new RuntimeException("Could not setValue [" + value + "]: " + e.getMessage(), e);
//        }
    }

    private Map<String, Object> getMap() {
        Map<String, Object> map = getValueImpl(getOwner().getDataObject());

        if (map == null && options.initialValue != null) {
            map = options.initialValue;
            getOwner().getDataObject().put(name, map);
        }
        return map;
    }

//    private Map<String, Object> getMapImpl(final DataObject dataObject) {
//        final JSONObject jsonObject;
//
//        if (dataObject.has(name)) {
//            jsonObject = dataObject.getJSONObject(name);
//        } else {
//            jsonObject = new JSONObject();
//            dataObject.put(name, jsonObject);
//        }
//        return getInternalMapFromJSONObject(jsonObject);
//    }


    private Map<String, Object> getInternalMapFromJSONObject(final JSONObject jsonObject) {
        try {
            return (Map<String, Object>) getInternapArrayField().get(jsonObject);
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

    public Object get(final Object key) {
        return getMap().get(key);
    }

    public Set<String> keySet() {
        return getMap().keySet();
    }

    public Collection<Object> values() {
        return getMap().values();
    }

    public Set<Map.Entry<String, Object>> entrySet() {
        return getMap().entrySet();
    }


    // Modification Operations
    public Object put(final String key, final Object value) {
        return getMap().put(key, value);
    }

    public Object remove(final Object key) {
        return getMap().remove(key);
    }

    public void putAll(final Map<? extends String, ?> m) {
        getMap().putAll(m);
    }

    public void clear() {
        getMap().clear();
    }
}
