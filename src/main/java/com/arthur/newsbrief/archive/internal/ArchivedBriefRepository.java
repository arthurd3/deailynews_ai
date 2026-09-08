package com.arthur.newsbrief.archive.internal;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

interface ArchivedBriefRepository extends ListCrudRepository<ArchivedBriefRow, Long> {

    @Query("""
            SELECT * FROM archived_brief
            ORDER BY generated_at DESC
            LIMIT :limit
            """)
    List<ArchivedBriefRow> findRecent(@Param("limit") int limit);

    /**
     * Category is nullable, so it is compared with IS NOT DISTINCT FROM — plain equality
     * would never match the rows where no category was requested.
     */
    @Query("""
            SELECT * FROM archived_brief
            WHERE day = :day
              AND country = :country
              AND category IS NOT DISTINCT FROM :category
            ORDER BY generated_at DESC
            LIMIT 1
            """)
    Optional<ArchivedBriefRow> findForDay(@Param("day") LocalDate day,
                                          @Param("country") String country,
                                          @Param("category") String category);
}
