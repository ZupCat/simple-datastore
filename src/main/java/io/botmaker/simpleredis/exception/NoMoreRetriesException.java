package io.botmaker.simpleredis.exception;

import java.io.Serializable;

/**
 * Used as a mark for Retrying algorithms
 */
public final class NoMoreRetriesException extends RuntimeException implements Serializable {

    private static final long serialVersionUID = -7034897190745766939L;

    public NoMoreRetriesException(final Throwable cause) {
        super(cause);
    }
}
