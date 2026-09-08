/**
 * Keeps generated briefs so they outlive the process and can be read back later.
 *
 * <p>Fed entirely by events. The brief module publishes {@code BriefGenerated}; nothing in it
 * imports anything from here. That is the point: archiving can be added, changed or removed
 * without touching the code that generates briefs.
 */
@ApplicationModule(displayName = "Brief Archive")
package com.arthur.newsbrief.archive;

import org.springframework.modulith.ApplicationModule;
