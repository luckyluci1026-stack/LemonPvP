package dev.lemonpvp.test.library;

/**
 * Service published by test-library. A module sees this type only if it declares a
 * dependency on test-library; the class is then loaded once, by the library's class loader.
 */
public interface CounterService {

    /**
     * @param name counter name
     * @return the new value
     */
    long increment(String name);

    /** @return version of the test-library instance that provides this service */
    String providerVersion();
}
