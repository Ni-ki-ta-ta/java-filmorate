MERGE INTO mpa (mpa_id, name)
    KEY (mpa_id)
    VALUES
    (1, '0+'),
    (2, '6+'),
    (3, '12+'),
    (4, '16+'),
    (5, '18+');

MERGE INTO genres (genre_id, name)
    KEY (genre_id)
    VALUES
    (1, 'Комедия'),
    (2, 'Драма'),
    (3, 'Мультфильм'),
    (4, 'Триллер'),
    (5, 'Документальный'),
    (6, 'Боевик');
