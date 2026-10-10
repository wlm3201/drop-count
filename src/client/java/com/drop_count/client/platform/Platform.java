package com.drop_count.client.platform;

/**
 * Entry point to the version specific implementation, see versions/<version>/src.
 */
public final class Platform {

    private static final PlatformHooksImpl HOOKS = new PlatformHooksImpl();

    private Platform() {
    }

    public static PlatformHooks hooks() {
        return HOOKS;
    }
}
