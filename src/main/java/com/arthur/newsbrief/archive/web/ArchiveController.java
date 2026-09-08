package com.arthur.newsbrief.archive.web;

import java.util.List;

import com.arthur.newsbrief.archive.ArchivedBrief;
import com.arthur.newsbrief.archive.BriefArchive;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Publishes the archive.
 *
 * <p>These routes live in the archive module rather than alongside the other pages on purpose.
 * The archive already depends on the brief module for the event type it consumes; having the
 * brief module reach back for {@link BriefArchive} would close a cycle, and
 * {@code ModularityTests} would fail the build. A module owning its own endpoints avoids that
 * and keeps the dependency pointing one way.
 */
@Controller
@Tag(name = "Archive", description = "Briefs generated on previous days")
class ArchiveController {

    private final BriefArchive archive;

    ArchiveController(BriefArchive archive) {
        this.archive = archive;
    }

    @GetMapping("/archive")
    String archivePage(@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit, Model model) {
        model.addAttribute("briefs", archive.recent(limit));
        model.addAttribute("total", archive.count());
        return "archive";
    }

    @GetMapping(value = "/api/v1/archive", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Operation(summary = "Recently archived briefs, newest first")
    List<ArchivedBrief> recent(@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return archive.recent(limit);
    }
}
