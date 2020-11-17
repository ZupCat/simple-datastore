package io.botmaker.simpleredis.model;

import java.io.Serializable;
import java.util.logging.Logger;

/**
 * Base class for all persistent entities. Handles data and metadata of business objects
 */
public abstract class PersistentObject implements Serializable {

    public static final String DATE_FORMAT = "yyMMddHHmmssSSS";
    protected static final Logger LOGGER = Logger.getLogger(PersistentObject.class.getName());
    private static final long serialVersionUID = 6181606486836703354L;
    private boolean isPersisted;

    public abstract String getId();

    public abstract void setId(final String id);

    public abstract void setModified();

    public void resetRedisAttributes() {
        // nothing to do
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final PersistentObject that = (PersistentObject) o;
        final String myId = getId();
        final String thatId = that.getId();

        return !(myId != null ? !myId.equals(thatId) : thatId != null);
    }

    @Override
    public int hashCode() {
        final String id = getId();

        return id != null ? id.hashCode() : 0;
    }

    public void isPersisted(final boolean isPersisted) {
        this.isPersisted = isPersisted;
    }

    public boolean isPersisted() {
        return isPersisted;
    }
}
