package com.arthur.newsbrief;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Turns the architecture into a build step.
 *
 * <p>Module boundaries that are only written down get violated; boundaries that fail the
 * build do not. If any module reaches into a sibling's {@code internal} package, this
 * test breaks and says exactly which import did it.
 */
class ModularityTests {

    static final ApplicationModules MODULES = ApplicationModules.of(NewsBriefApplication.class);

    @Test
    void modulesRespectTheirBoundaries() {
        MODULES.verify();
    }

    /**
     * Regenerates the C4 diagrams and module canvases from the code, into the build
     * directory rather than the repository - they are output, not source.
     */
    @Test
    void writesModuleDocumentation() {
        new Documenter(MODULES, Documenter.Options.defaults().withOutputFolder("target/modules"))
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleCanvases();
    }
}
