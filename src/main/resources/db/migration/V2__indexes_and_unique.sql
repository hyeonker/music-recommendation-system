create index if not exists idx_songs_title  on songs (title);
create index if not exists idx_songs_artist on songs (artist);
alter table songs
    add constraint uk_songs_title_artist unique (title, artist);
