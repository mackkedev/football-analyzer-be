INSERT INTO league (name, country, api_league_id, current_season, api_competition_code) VALUES
    ('Premier League', 'England', 39, 2025, 'PL'),
    ('Serie A', 'Italy', 135, 2025, 'SA'),
    ('Bundesliga', 'Germany', 78, 2025, 'BL1'),
    ('La Liga', 'Spain', 140, 2025, 'PD'),
    ('Championship', 'England', 40, 2025, 'ELC')
ON CONFLICT (api_league_id) DO UPDATE SET
    current_season = EXCLUDED.current_season,
    api_competition_code = EXCLUDED.api_competition_code;
