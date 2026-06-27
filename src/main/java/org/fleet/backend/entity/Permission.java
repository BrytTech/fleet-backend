package org.fleet.backend.entity;

public enum Permission {
    // Customer permissions
    ORDER_CREATE,
    ORDER_VIEW,
    ORDER_CANCEL,
    ORDER_TRACK,
    RIDER_RATE,

    // Rider permissions
    ORDER_VIEW_NEARBY,
    ORDER_ACCEPT,
    ORDER_UPDATE_STATUS,
    ORDER_HISTORY_VIEW,
    EARNINGS_VIEW,

    //Admin permissions
    USER_VIEW_ALL,
    USER_BLOCK,
    RIDER_VERIFY,
    ORDER_VIEW_ALL,
    DISPUTE_RESOLVE
}
