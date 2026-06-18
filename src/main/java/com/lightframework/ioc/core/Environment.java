package com.lightframework.ioc.core;

/**
 * Interface representing the environment in which the application runs.
 * Provides access to active profiles and profile-based conditional logic.
 */
public interface Environment {
    
    /**
     * @return the set of currently active profiles
     */
    String[] getActiveProfiles();
    
    /**
     * Set the active profiles, replacing any previously set profiles.
     *
     * @param profiles the profiles to activate
     */
    void setActiveProfiles(String... profiles);
    
    /**
     * Add a profile to the set of active profiles.
     *
     * @param profile the profile to add
     */
    void addActiveProfile(String profile);
    
    /**
     * Check whether one or more specified profiles are active.
     *
     * @param profiles the profiles to check
     * @return true if at least one of the given profiles is active, or if the array is empty
     */
    boolean acceptsProfiles(String... profiles);
    
    /**
     * @return the name of the default profile
     */
    String getDefaultProfile();
}
