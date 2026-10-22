package com.dropcount.client.platform;

public final class Platform {

    private static final PlatformHooksImpl HOOKS = new PlatformHooksImpl();

    private Platform() {}

    public static PlatformHooks hooks() {
        return HOOKS;
    }
}
