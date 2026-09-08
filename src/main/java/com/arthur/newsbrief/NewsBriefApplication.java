package com.arthur.newsbrief;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point for the NewsBrief AI service.
 *
 * <p>The application is a modular monolith: every direct sub-package of this one is an
 * {@link org.springframework.modulith.ApplicationModule application module} whose
 * {@code internal} sub-package is off-limits to its siblings. {@code ModularityTests}
 * fails the build when that rule is broken.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class NewsBriefApplication {

    public static void main(String[] args) {
        SpringApplication.run(NewsBriefApplication.class, args);
    }
}
