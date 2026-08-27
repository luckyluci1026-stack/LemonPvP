package com.lemonpvp.lemonfailover;

/** The two operating modes of the fallback proxy. */
public enum FailoverState {
    /** Primary proxy is healthy — this proxy stays out of the way. */
    STANDBY,
    /** Primary proxy is unreachable — this proxy has taken over. */
    ACTIVE
}
