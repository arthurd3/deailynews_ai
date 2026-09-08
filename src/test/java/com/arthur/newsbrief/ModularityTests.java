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

    @Test
    void writesModuleDocumentation() {
        new Documenter(MODULES, Documenter.Options.defaults().withOutputFolder("docs/modules"))
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleCanvases();
    }
}
