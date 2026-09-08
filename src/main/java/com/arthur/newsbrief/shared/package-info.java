/**
 * Cross-cutting infrastructure every other module is allowed to depend on: cache setup,
 * API documentation, resilience wiring and the shared error vocabulary.
 *
 * <p>Declared {@link ApplicationModule.Type#OPEN} so its nested packages stay reachable
 * from the feature modules. Nothing feature-specific belongs here — a module's own
 * configuration properties live in that module's {@code internal} package.
 */
@ApplicationModule(displayName = "Shared Kernel", type = ApplicationModule.Type.OPEN)
package com.arthur.newsbrief.shared;

import org.springframework.modulith.ApplicationModule;
