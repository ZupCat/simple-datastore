package io.botmaker.simpleredis.property;

import io.botmaker.simpleredis.model.DataObject;
import io.botmaker.simpleredis.model.RedisEntity;
import io.botmaker.simpleredis.model.config.PropertyMeta;

import java.io.Serializable;

public final class IntegerProperty extends PropertyMeta<Integer> implements Serializable {

    private static final long serialVersionUID = 6181606486836703354L;


    public IntegerProperty(final RedisEntity owner) {
        super(owner);
    }

    protected Integer getValueImpl(final DataObject dataObject) {
        final Object r = dataObject.opt(name);
        return r == null ? null : ((Number) r).intValue();

        // NOTE using same implementation that opt method (with try/catch)
//        try {
//            return dataObject.getInt(name);
//        } catch (Exception e) {
//            return null;
//        }
    }

    protected void setValueImpl(final Integer value, final DataObject dataObject) {
        dataObject.put(name, value);
    }

    @Override
    public void setFromStringValue(final String stringValue, final boolean forceAudit) {
        set(stringValue == null || stringValue.trim().length() == 0 ? null : Integer.parseInt(stringValue), forceAudit);
    }

    public void add(final int value) {
        final Integer current = get();
        this.set(current == null ? value : (current + value));
    }

    public void substract(final int value) {
        this.add(-value);
    }

    public void decrement() {
        this.substract(1);
    }

    public void increment() {
        this.add(1);
    }

    public boolean isNullOrZero() {
        return this.get() == null || this.get() == 0;
    }
}
