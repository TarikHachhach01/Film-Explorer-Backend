package com.isfa.dsi.filmexplorer.user;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Permission {
    READ_MOVIES("read:movies"),
    WRITE_REVIEWS("write:reviews"),
    READ_REVIEWS("read:reviews"),
    DELETE_REVIEWS("delete:reviews"),
    WRITEE_MOVIES("write:movies"),
    MANAGE_USERS("manage:users");


    private final String permission;
}