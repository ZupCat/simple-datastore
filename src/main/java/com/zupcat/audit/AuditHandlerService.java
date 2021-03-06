package com.zupcat.audit;

import com.zupcat.model.config.PropertyMeta;

/**
 * Callback called when an auditable object property is changed.
 * A common usage is calling BigQuery to store that data
 */
public interface AuditHandlerService {

    void logPropertyDataChanged(final PropertyMeta property, final Object newValue, final String ownerId);
}
