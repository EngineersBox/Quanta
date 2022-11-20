package com.engineersbox.quanta.debug.hooks;

public interface InstanceIdentifierProvider {

    static String deriveInstanceID(final Object instance) {
        if (instance instanceof InstanceIdentifierProvider iip) {
            return iip.provideInstanceID();
        }
        return String.format(
                "%s@%s",
                instance.getClass().getSimpleName(),
                Integer.toHexString(instance.hashCode())
        );
    }

    String provideInstanceID();

}
