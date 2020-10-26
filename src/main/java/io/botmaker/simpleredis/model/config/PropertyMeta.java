package io.botmaker.simpleredis.model.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.botmaker.simpleredis.audit.AuditHandlerServiceFactory;
import io.botmaker.simpleredis.model.DataObject;
import io.botmaker.simpleredis.model.RedisEntity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Holds metadata of busines objects properties
 */
public abstract class PropertyMeta<E> implements Serializable {

    private static final long serialVersionUID = 6181606486836703354L;
    @JsonIgnore
    private final RedisEntity owner;
    protected String name;
    protected AbstractPropertyBuilder<? extends PropertyMeta<E>, E> options;

    protected PropertyMeta(final RedisEntity owner) {
        this.owner = owner;
    }

    public void config(final AbstractPropertyBuilder<? extends PropertyMeta<E>, E> options) {
        this.options = options;
    }

    public String getPropertyName() {
        return name;
    }

    public void setPropertyName(final String propertyName) {
        this.name = propertyName;
    }

    protected RedisEntity getOwner() {
        return owner;
    }

    public boolean isFullyEquals(final PropertyMeta other) {
        // skipping owner comparisson. It is not needed and causes stackoverflow
        return !(other == null ||
                !Objects.equals(this.name, other.name) ||
                !Objects.equals(options.initialValue, other.options.initialValue) ||
                options.sendToClient != other.options.sendToClient ||
                options.auditable != other.options.auditable ||
                options.compressGzip != other.options.compressGzip ||
                options.indexable != other.options.indexable ||
                options.uniqueIndex != other.options.uniqueIndex);
    }

    public boolean isIndexable() {
        return options.indexable;
    }

    public boolean isUniqueIndex() {
        return options.uniqueIndex;
    }

//    public E getInitialValue() {
//        return initialValue;
//    }

    public boolean hasToSendToClient() {
        return options.sendToClient;
    }

    public E get() {
        final E result = getValueImpl(owner.getDataObject());

        return result == null ? options.initialValue : result;
    }

    public void set(final E value) {
        if (options.inlinePrePropertyChangeObserver != null) {
            options.inlinePrePropertyChangeObserver.execute(value);
        }

        final E oldValue = get();
        this.set(value, options.auditable);

        if (options.inlinePosPropertyChangeObserver != null) {
            options.inlinePosPropertyChangeObserver.execute(oldValue);
        }
    }

    public void set(final E value, final boolean forceAudit) {
        if (forceAudit) {
            AuditHandlerServiceFactory.getAuditHandler().logPropertyDataChanged(this, value, owner.getId());
        }

        final DataObject dataObject = owner.getDataObject();

        if (value == null || value.equals(options.initialValue)) {
            dataObject.remove(name);
        } else {
            setValueImpl(value, dataObject);
        }
    }

    public AbstractPropertyBuilder<? extends PropertyMeta<E>, E> getOptions() {
        return options;
    }

    protected abstract E getValueImpl(final DataObject dataObject);

    protected abstract void setValueImpl(final E value, final DataObject dataObject);

    public void setFromStringValue(final String stringValue) {
        setFromStringValue(stringValue, options.auditable);
    }

    public abstract void setFromStringValue(final String stringValue, final boolean forceAudit);

    @Override
    public String toString() {
        return name + ":" + get();
    }
}
