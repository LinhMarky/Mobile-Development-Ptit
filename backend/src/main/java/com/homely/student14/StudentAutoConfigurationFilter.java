package com.homely.student14;

import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/** The JDBC runtime does not use SpringFilter's JPA components. */
public class StudentAutoConfigurationFilter
        implements AutoConfigurationImportFilter, EnvironmentAware {
    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public boolean[] match(String[] configurations, AutoConfigurationMetadata metadata) {
        boolean student14 = this.environment.acceptsProfiles(Profiles.of("student14"));
        boolean[] matches = new boolean[configurations.length];

        for (int index = 0; index < configurations.length; index++) {
            String name = configurations[index];
            matches[index] =
                    !student14 || name == null || !name.startsWith("com.turkraft.springfilter.");
        }

        return matches;
    }
}
