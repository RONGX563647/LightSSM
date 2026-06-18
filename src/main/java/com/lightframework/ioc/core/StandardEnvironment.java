package com.lightframework.ioc.core;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Standard implementation of the Environment interface.
 * Reads active profiles from system properties and falls back to "default".
 */
public class StandardEnvironment implements Environment {
    
    private static final String DEFAULT_PROFILE = "default";
    private static final String ACTIVE_PROFILES_PROPERTY = "light.profiles.active";
    
    private final Set<String> activeProfiles = new LinkedHashSet<>();
    
    public StandardEnvironment() {
        // Read active profiles from system properties
        String profiles = System.getProperty(ACTIVE_PROFILES_PROPERTY);
        if (profiles != null && !profiles.isEmpty()) {
            setActiveProfiles(profiles.split(","));
        } else {
            addActiveProfile(DEFAULT_PROFILE);
        }
    }
    
    @Override
    public String[] getActiveProfiles() {
        return activeProfiles.toArray(new String[0]);
    }
    
    @Override
    public void setActiveProfiles(String... profiles) {
        activeProfiles.clear();
        for (String profile : profiles) {
            activeProfiles.add(profile.trim());
        }
    }
    
    @Override
    public void addActiveProfile(String profile) {
        activeProfiles.add(profile);
    }
    
    @Override
    public boolean acceptsProfiles(String... profiles) {
        if (profiles == null || profiles.length == 0) {
            return true;
        }
        for (String profile : profiles) {
            if (activeProfiles.contains(profile)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public String getDefaultProfile() {
        return DEFAULT_PROFILE;
    }
}
