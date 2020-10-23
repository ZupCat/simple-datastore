package io.botmaker.simpleredis.model;

import io.botmaker.simpleredis.model.config.DATE;
import io.botmaker.simpleredis.model.config.PropertyMeta;
import io.botmaker.simpleredis.model.config.STRING;
import io.botmaker.simpleredis.property.ByteArrayProperty;
import io.botmaker.simpleredis.property.DateProperty;
import io.botmaker.simpleredis.property.ListPrimitiveObjectProperty;
import io.botmaker.simpleredis.property.StringProperty;
import io.botmaker.simpleredis.util.RandomUtils;
import io.botmaker.simpleredis.util.TimeUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang.ArrayUtils;
import org.json.JSONObject;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public abstract class RedisEntity extends PersistentObject implements Serializable {

    public static final int MAX_GROUPS = 100;

    public static final int EXPIRING_5_MINUTES = 60 * 5;
    public static final int EXPIRING_10_MINUTES = 60 * 10;
    public static final int EXPIRING_1_HOUR = 60 * 60;
    public static final int EXPIRING_1_DAY = EXPIRING_1_HOUR * 24;
    public static final int EXPIRING_1_WEEK = EXPIRING_1_DAY * 7;
    public static final int EXPIRING_1_MONTH = EXPIRING_1_WEEK * 4;
    public static final int EXPIRING_6_MONTHS = EXPIRING_1_MONTH * 6;
    public static final int EXPIRING_1_YEAR = EXPIRING_1_MONTH * 12;
    public static final int EXPIRING_3_YEARS = EXPIRING_1_YEAR * 3;
    public static final int EXPIRING_NEVER = 0;

    private static final long serialVersionUID = 6181606486836703354L;
    protected final Map<String, PropertyMeta> propertiesMetadata = new ConcurrentHashMap<>();
    // persistent state
    private final DataObject dataObject = new DataObject();
    private final String entityName;
    private final int secondsToExpire; // 0 means never
    private final boolean usesAppIdPrefix;
    private String tempId;
    // entity usefull properties

    //    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
//    public IntegerProperty GROUP_ID;
    public DateProperty LAST_MODIFICATION;

    public StringProperty OBJECT_TYPE;


    protected RedisEntity(final RedisEntity template) {
        isPersisted(template.isPersisted());

        propertiesMetadata.putAll(template.propertiesMetadata);
        entityName = template.entityName;
        secondsToExpire = template.secondsToExpire;
        usesAppIdPrefix = template.usesAppIdPrefix;

        dataObject.mergeWith(template.dataObject);
    }

    protected RedisEntity(final boolean usesAppIdPrefix, final int secondsToExpire) {
        entityName = getEntityName(this.getClass());

        setNewId();

        this.secondsToExpire = secondsToExpire;
        this.usesAppIdPrefix = usesAppIdPrefix;

//        GROUP_ID = new INT(this).indexable().build();
        LAST_MODIFICATION = new DATE(this).sendToClient().mandatory().build();
        OBJECT_TYPE = new STRING(this).sendToClient().mandatory().build();

        config();

        for (final Field field : getClass().getFields()) {
            if (PropertyMeta.class.isAssignableFrom(field.getType())) {
                final String propertyName = field.getName();
                field.setAccessible(true);
                try {
                    final PropertyMeta propertyMeta = (PropertyMeta) field.get(this);

                    propertyMeta.setPropertyName(propertyName);
                    addPropertyMeta(propertyName, propertyMeta);

                } catch (final IllegalAccessException _illegalAccessException) {
                    throw new RuntimeException("Problems getting value for field [" + propertyName + "], of class [" + getClass() + "]. Possible private variable?: " + _illegalAccessException.getMessage(), _illegalAccessException);
                } finally {
                    field.setAccessible(false);
                }
            }
        }

        setModified();

//        GROUP_ID.set(Math.abs(getId().hashCode() % MAX_GROUPS));
        OBJECT_TYPE.set(getEntityName());
    }

    public static String getEntityName(final Class<? extends RedisEntity> clazz) {
        final String className = clazz.getName();
        return className.substring(className.lastIndexOf(".") + 1);
    }

    public static String createNewId() {
        return RandomUtils.getInstance().getRandomSafeAlphaNumberString(20);
    }

    public double getSortingScore() {
        return TimeUtils.fromISODate(this.LAST_MODIFICATION.get()).getTime();
    }

    public int getSecondsToExpire() {
        return secondsToExpire;
    }

    public void setNewId() {
        setId(createNewId());
    }

    public boolean shouldBeSentToClient() {
        for (final PropertyMeta propertyMeta : propertiesMetadata.values()) {
            if (propertyMeta.hasToSendToClient()) {
                return true;
            }
        }
        return false;
    }

    public boolean usesAppIdPrefix() {
        return usesAppIdPrefix;
    }

    @Override
    public void setModified() {
        LAST_MODIFICATION.set(TimeUtils.getCurrentAsISO());
    }

    public String getEntityName() {
        return entityName;
    }

    public boolean isFullyEquals(final RedisEntity other) {
        if (
                other == null ||
                        !Objects.equals(this.getId(), other.getId()) ||
                        !Objects.equals(this.entityName, other.entityName) ||
//                        !Objects.equals(this.GROUP_ID.get(), other.GROUP_ID.get()) ||
                        !Objects.equals(this.LAST_MODIFICATION.get(), other.LAST_MODIFICATION.get()) ||
                        this.propertiesMetadata.size() != other.propertiesMetadata.size() ||
                        !comparePropertiesWith(other)
        ) {
            return false;
        }

        for (final Map.Entry<String, PropertyMeta> entry : this.propertiesMetadata.entrySet()) {
            if (!entry.getValue().isFullyEquals(other.propertiesMetadata.get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    private boolean comparePropertiesWith(final RedisEntity anotherRedisEntity) {
        if (anotherRedisEntity == null) {
            return false;
        }

        if (this.propertiesMetadata.size() != anotherRedisEntity.propertiesMetadata.size()) {
            return false;
        }

        if (this.propertiesMetadata.isEmpty()) {
            return true;
        }

        for (final Map.Entry<String, PropertyMeta> myEntries : this.propertiesMetadata.entrySet()) {
            final PropertyMeta anotherPropertyMeta = anotherRedisEntity.propertiesMetadata.get(myEntries.getKey());

            if (anotherPropertyMeta == null) {
                return false;
            }

            final PropertyMeta object1Property = myEntries.getValue();
            final Object object1 = object1Property.get();
            final Object object2 = anotherPropertyMeta.get();

            if (object1Property instanceof ByteArrayProperty) {
                if (!ArrayUtils.isEquals(object1, object2)) {
                    return false;
                }
            } else if (object1Property instanceof ListPrimitiveObjectProperty) {
                if (!ListUtils.isEqualList((Collection<?>) object1, (Collection<?>) object2)) {
                    return false;
                }
            } else {
                if (!Objects.equals(object1, object2)) {
                    return false;
                }
            }
        }
        return true;
    }

    public DataObject getDataObjectForClient() {
        final DataObject result = new DataObject();
        final DataObject source = getDataObject();

        if (source != null) result.mergeWith(new JSONObject(source.getInternalMap()));

        for (final Map.Entry<String, PropertyMeta> entry : getPropertiesMetadata().entrySet()) {
            final PropertyMeta propertyMeta = entry.getValue();

            if (!propertyMeta.hasToSendToClient()) {
                result.remove(entry.getKey());
            }
        }
        return result;
    }

    public PropertyMeta getIndexablePropertyByName(final String name) {
        final Optional<PropertyMeta> result = getIndexableProperties().stream().filter(p -> p.getPropertyName().equals(name)).findFirst();

        return result.isPresent() ? result.get() : null;
    }

    public List<PropertyMeta> getIndexableProperties() {
        return propertiesMetadata
                .values().stream()
                .filter(PropertyMeta::isIndexable)
                .collect(Collectors.toList());
    }

    @Override
    public String getId() {
        if (tempId != null)
            return tempId;

        final Object r = getDataObject().opt(WithIdDataObject.ID_KEY);

        return r == null ? null : r.toString();
    }

    @Override
    public void setId(final String id) {
        getDataObject().put(WithIdDataObject.ID_KEY, id);
        tempId = id;
    }

    public Map<String, PropertyMeta> getPropertiesMetadata() {
        return propertiesMetadata;
    }

    public void addPropertyMeta(final String name, final PropertyMeta propertyMeta) {
//        if (propertyMeta.isIndexable() && propertyMeta.getInitialValue() == null) {
//            throw new RuntimeException("Property [" + name + "] of Entity [" + entityName + "] is indexable and has null default value. This is not allowed. Please change then initialValue to be not null");
//        }
        propertiesMetadata.put(name, propertyMeta);
    }

    public DataObject getDataObject() {
        return dataObject;
    }

    @Override
    public String toString() {
        return getDataObjectForClient().toString();
    }

    protected abstract void config();

    public int getDaysSinceLastModification() {
        return getMinutesSinceLastModification() / (60 * 24);
    }

    public int getMinutesSinceLastModification() {
        final long lm = LAST_MODIFICATION.getTicks();

        if (lm == 0L) {
            return 0;
        }

        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

        final GregorianCalendar now = new GregorianCalendar();
        now.setTimeZone(TimeZone.getTimeZone("GMT"));
        final GregorianCalendar modified = new GregorianCalendar();
        modified.setTimeZone(TimeZone.getTimeZone("GMT"));

        try {
            now.setTime(dateFormat.parse(Long.toString(TimeUtils.buildStandardModificationTime())));
            modified.setTime(dateFormat.parse(Long.toString(lm)));

        } catch (final ParseException _parseException) {
            throw new RuntimeException("Problems when getting minutesSinceLastModification with value [" + lm + "]. Expected format [" + DATE_FORMAT + "]: " + _parseException.getMessage(), _parseException);
        }

        return ((int) (Math.abs((now.getTime().getTime() - modified.getTime().getTime())) / 60000));
    }

    public Date getLastModificationAsDate() {
        try {
            return new SimpleDateFormat(DATE_FORMAT).parse(Long.toString(LAST_MODIFICATION.getTicks()));
        } catch (final ParseException _parseException) {
            throw new RuntimeException("Problems parsing date with value [" + LAST_MODIFICATION.get() + "]. Expected format [" + DATE_FORMAT + "]: " + _parseException.getMessage(), _parseException);
        }
    }

    public boolean hasAccessedToday() {
        final String lastAccessDate = Long.toString(LAST_MODIFICATION.getTicks()).substring(0, 6);
        final String today = Long.toString(TimeUtils.buildStandardModificationTime()).substring(0, 6);

        return lastAccessDate.equals(today);
    }
}
