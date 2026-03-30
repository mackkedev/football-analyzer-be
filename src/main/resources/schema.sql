-- ============================================
-- LEAGUES
-- ============================================
CREATE TABLE IF NOT EXISTS league (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    country         VARCHAR(50) NOT NULL,
    api_league_id   INTEGER NOT NULL UNIQUE,
    current_season  INTEGER NOT NULL,
    logo_url        VARCHAR(500),
    api_competition_code VARCHAR(5),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================
-- TEAMS
-- ============================================
CREATE TABLE IF NOT EXISTS team (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    short_name      VARCHAR(10),
    logo_url        VARCHAR(500),
    api_team_id     INTEGER NOT NULL UNIQUE,
    league_id       BIGINT NOT NULL REFERENCES league(id),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_team_league ON team(league_id);
CREATE INDEX IF NOT EXISTS idx_team_api_id ON team(api_team_id);

-- ============================================
-- GAMEWEEKS
-- ============================================
CREATE TABLE IF NOT EXISTS gameweek (
    id              BIGSERIAL PRIMARY KEY,
    league_id       BIGINT NOT NULL REFERENCES league(id),
    round_number    INTEGER NOT NULL,
    label           VARCHAR(100),
    start_date      DATE,
    end_date        DATE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(league_id, round_number)
);

CREATE INDEX IF NOT EXISTS idx_gameweek_league ON gameweek(league_id);

-- ============================================
-- MATCHES
-- ============================================
CREATE TABLE IF NOT EXISTS match (
    id              BIGSERIAL PRIMARY KEY,
    league_id       BIGINT NOT NULL REFERENCES league(id),
    gameweek_id     BIGINT REFERENCES gameweek(id),
    home_team_id    BIGINT NOT NULL REFERENCES team(id),
    away_team_id    BIGINT NOT NULL REFERENCES team(id),
    api_fixture_id  INTEGER NOT NULL UNIQUE,
    kickoff_time    TIMESTAMP NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_match_league ON match(league_id);
CREATE INDEX IF NOT EXISTS idx_match_gameweek ON match(gameweek_id);
CREATE INDEX IF NOT EXISTS idx_match_kickoff ON match(kickoff_time);
CREATE INDEX IF NOT EXISTS idx_match_status ON match(status);
CREATE INDEX IF NOT EXISTS idx_match_home_team ON match(home_team_id);
CREATE INDEX IF NOT EXISTS idx_match_away_team ON match(away_team_id);

-- ============================================
-- MATCH RESULTS
-- ============================================
CREATE TABLE IF NOT EXISTS match_result (
    id                      BIGSERIAL PRIMARY KEY,
    match_id                BIGINT NOT NULL UNIQUE REFERENCES match(id),
    home_goals_1st_half     INTEGER,
    away_goals_1st_half     INTEGER,
    home_goals_2nd_half     INTEGER,
    away_goals_2nd_half     INTEGER,
    home_goals_total        INTEGER NOT NULL,
    away_goals_total        INTEGER NOT NULL,
    home_corners            INTEGER,
    away_corners            INTEGER,
    home_yellow_cards       INTEGER,
    away_yellow_cards       INTEGER,
    home_red_cards          INTEGER,
    away_red_cards          INTEGER,
    full_time_result        VARCHAR(1) NOT NULL, -- '1', 'X', '2'
    btts                    BOOLEAN NOT NULL,
    more_goals_2nd_half     BOOLEAN,
    goals_total             DECIMAL(4,1) NOT NULL,
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_match_result_match ON match_result(match_id);

-- ============================================
-- TEAM FORM (rolling stats)
-- ============================================
CREATE TABLE IF NOT EXISTS team_form (
    id                          BIGSERIAL PRIMARY KEY,
    team_id                     BIGINT NOT NULL REFERENCES team(id),
    league_id                   BIGINT NOT NULL REFERENCES league(id),
    season                      INTEGER NOT NULL,
    matches_played              INTEGER NOT NULL DEFAULT 0,
    wins                        INTEGER NOT NULL DEFAULT 0,
    draws                       INTEGER NOT NULL DEFAULT 0,
    losses                      INTEGER NOT NULL DEFAULT 0,
    goals_scored                INTEGER NOT NULL DEFAULT 0,
    goals_conceded              INTEGER NOT NULL DEFAULT 0,
    avg_goals_scored            DECIMAL(4,2) DEFAULT 0,
    avg_goals_conceded          DECIMAL(4,2) DEFAULT 0,
    avg_corners_for             DECIMAL(4,2) DEFAULT 0,
    avg_corners_against         DECIMAL(4,2) DEFAULT 0,
    avg_yellow_cards            DECIMAL(4,2) DEFAULT 0,
    avg_red_cards               DECIMAL(4,2) DEFAULT 0,
    btts_percentage             DECIMAL(5,2) DEFAULT 0,
    over_25_percentage          DECIMAL(5,2) DEFAULT 0,
    more_goals_2nd_half_pct     DECIMAL(5,2) DEFAULT 0,
    home_wins                   INTEGER DEFAULT 0,
    home_draws                  INTEGER DEFAULT 0,
    home_losses                 INTEGER DEFAULT 0,
    away_wins                   INTEGER DEFAULT 0,
    away_draws                  INTEGER DEFAULT 0,
    away_losses                 INTEGER DEFAULT 0,
    last_5_form                 VARCHAR(5),
    league_position             INTEGER,
    updated_at                  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(team_id, league_id, season)
);

CREATE INDEX IF NOT EXISTS idx_team_form_team ON team_form(team_id);
CREATE INDEX IF NOT EXISTS idx_team_form_league_season ON team_form(league_id, season);

-- ============================================
-- ANALYSIS (AI predictions)
-- ============================================
CREATE TABLE IF NOT EXISTS analysis (
    id                              BIGSERIAL PRIMARY KEY,
    match_id                        BIGINT NOT NULL UNIQUE REFERENCES match(id),
    result_prediction               VARCHAR(3) NOT NULL,    -- '1', 'X', '2', '1,X', 'X,2', '1,2'
    result_confidence               DECIMAL(3,2) NOT NULL,
    btts_prediction                 BOOLEAN NOT NULL,
    btts_confidence                 DECIMAL(3,2) NOT NULL,
    more_goals_2nd_half_prediction  BOOLEAN NOT NULL,
    more_goals_2nd_half_confidence  DECIMAL(3,2) NOT NULL,
    most_shots_on_goal              VARCHAR(4),             -- 'HOME' or 'AWAY'
    most_yellow_cards               VARCHAR(4),             -- 'HOME' or 'AWAY'
    most_corners                    VARCHAR(4),             -- 'HOME' or 'AWAY'
    reasoning                       TEXT,
    raw_ai_response                 TEXT,
    model_used                      VARCHAR(50),
    analyzed_at                     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_analysis_match ON analysis(match_id);

-- ============================================
-- PREDICTION ACCURACY
-- ============================================
CREATE TABLE IF NOT EXISTS prediction_accuracy (
    id                          BIGSERIAL PRIMARY KEY,
    analysis_id                 BIGINT NOT NULL UNIQUE REFERENCES analysis(id),
    match_id                    BIGINT NOT NULL REFERENCES match(id),
    result_correct              BOOLEAN,
    btts_correct                BOOLEAN,
    more_goals_2nd_half_correct BOOLEAN,
    shots_correct               BOOLEAN,    -- null if shots data unavailable
    yellow_cards_correct        BOOLEAN,
    corners_correct             BOOLEAN,
    score                       INTEGER,    -- points out of 5 (shots excluded when null)
    evaluated_at                TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_accuracy_match ON prediction_accuracy(match_id);
CREATE INDEX IF NOT EXISTS idx_accuracy_analysis ON prediction_accuracy(analysis_id);

-- ============================================
-- STRYKTIPSET DRAWS
-- ============================================
CREATE TABLE IF NOT EXISTS stryktipset_draw (
    id              BIGSERIAL PRIMARY KEY,
    draw_number     INTEGER NOT NULL UNIQUE,
    draw_state      VARCHAR(20) NOT NULL,
    reg_close_time  TIMESTAMP NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_stryktipset_draw_number ON stryktipset_draw(draw_number);
CREATE INDEX IF NOT EXISTS idx_stryktipset_draw_state ON stryktipset_draw(draw_state);

-- ============================================
-- STRYKTIPSET EVENTS (13 matches per draw)
-- ============================================
CREATE TABLE IF NOT EXISTS stryktipset_event (
    id              BIGSERIAL PRIMARY KEY,
    draw_id         BIGINT NOT NULL REFERENCES stryktipset_draw(id),
    event_number    INTEGER NOT NULL,
    home_team       VARCHAR(100) NOT NULL,
    away_team       VARCHAR(100) NOT NULL,
    league_name     VARCHAR(100),
    kickoff_time    TIMESTAMP,
    odds_1          DECIMAL(6,2),
    odds_x          DECIMAL(6,2),
    odds_2          DECIMAL(6,2),
    sf_1            DECIMAL(5,2),
    sf_x            DECIMAL(5,2),
    sf_2            DECIMAL(5,2),
    ai_prediction   VARCHAR(1),         -- '1', 'X', '2'
    ai_confidence   DECIMAL(3,2),
    ai_reasoning    TEXT,
    actual_result   VARCHAR(1),         -- '1', 'X', '2'
    home_goals      INTEGER,
    away_goals      INTEGER,
    coupon_signs    VARCHAR(5),
    coupon_reasoning TEXT,
    UNIQUE(draw_id, event_number)
);

CREATE INDEX IF NOT EXISTS idx_stryktipset_event_draw ON stryktipset_event(draw_id);
