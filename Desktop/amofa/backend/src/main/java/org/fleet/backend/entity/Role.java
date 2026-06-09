package org.fleet.backend.entity;

import java.util.Set;

public enum Role {

    CUSTOMER(Set.of(
             Permission.ORDER_CREATE,
             Permission.ORDER_VIEW,
             Permission.ORDER_CANCEL,
             Permission.ORDER_TRACK,
             Permission.RIDER_RATE
             )),

    RIDER(Set.of(
            Permission.ORDER_VIEW_NEARBY,
            Permission.ORDER_ACCEPT,
            Permission.ORDER_HISTORY_VIEW,
            Permission.EARNINGS_VIEW
    )),

    ADMIN(Set.of(
            Permission.USER_VIEW_ALL,
            Permission.USER_BLOCK,
            Permission.RIDER_VERIFY,
            Permission.ORDER_VIEW_ALL,
            Permission.DISPUTE_RESOLVE
    ));

    private final Set<Permission> permission;

    Role(Set<Permission> permission) {
        this.permission = permission;
    }

    public Set<Permission> getPermission() {
        return permission;
    }
}
