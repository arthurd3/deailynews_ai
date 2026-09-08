package com.arthur.newsbrief.archive;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Read side of the archive.
 *
 * <p>Deliberately query-only: nothing outside this module writes to the archive, because
 * writing is triggered by an event rather than by a caller.
 */
public interface BriefArchive {

    /** Most recently archived briefs, newest first. */
    List<ArchivedBrief> recent(int limit);

    /** The brief stored for a given day and selection, if there is one. */
    Optional<ArchivedBrief> forDay(LocalDate day, String country, String category);

    /** How many briefs the archive holds. */
    long count();
}
