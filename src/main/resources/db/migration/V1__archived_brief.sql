-- Briefs kept beyond the lifetime of the process.
--
-- Topics live in a JSON column rather than a child table: they are only ever read back whole,
-- alongside their brief, so a join would buy nothing. Postgres can still index into them later
-- if that ever changes.
CREATE TABLE archived_brief (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    day            DATE        NOT NULL,
    country        VARCHAR(2)  NOT NULL,
    category       VARCHAR(32),
    headline       TEXT        NOT NULL,
    overview       TEXT        NOT NULL,
    topics_json    TEXT        NOT NULL,
    article_count  INTEGER     NOT NULL,
    generated_at   TIMESTAMPTZ NOT NULL
);

-- Browsing the archive is always "most recent first", and looking a day up is the other query.
CREATE INDEX idx_archived_brief_generated_at ON archived_brief (generated_at DESC);
CREATE INDEX idx_archived_brief_day ON archived_brief (day DESC, country, category);
