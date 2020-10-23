package io.botmaker.simpleredis.property;

import io.botmaker.simpleredis.model.DataObject;
import io.botmaker.simpleredis.model.RedisEntity;
import io.botmaker.simpleredis.model.config.PropertyMeta;

import java.io.Serializable;

public final class BooleanProperty extends PropertyMeta<Boolean> implements Serializable {

    private static final long serialVersionUID = 6181606486836703354L;


    public BooleanProperty(final RedisEntity owner) {
        super(owner);
    }

    protected Boolean getValueImpl(final DataObject dataObject) {
        return (Boolean) dataObject.opt(name);
        // NOTE using same implementation that opt method (with try/catch)
//        try {
//            return dataObject.getBoolean(name);
//        } catch (Exception e) {
//            return null;
//        }
    }

    @Override
    public void setFromStringValue(final String stringValue, final boolean forceAudit) {
        set(stringValue == null || stringValue.trim().length() == 0 ? null : Boolean.parseBoolean(stringValue), forceAudit);
    }

    protected void setValueImpl(final Boolean value, final DataObject dataObject) {
        dataObject.put(name, value);
    }
}
