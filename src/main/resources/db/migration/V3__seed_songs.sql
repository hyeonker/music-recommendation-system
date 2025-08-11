-- 유니크(uk_songs_title_artist) 충돌이면 건너뜀
insert into songs (title, artist)
values ('So What', 'Miles Davis')
    on conflict on constraint uk_songs_title_artist do nothing;

insert into songs (title, artist)
values ('Time', 'Hans Zimmer')
    on conflict on constraint uk_songs_title_artist do nothing;

insert into songs (title, artist)
values ('Shape of You', 'Ed Sheeran')
    on conflict on constraint uk_songs_title_artist do nothing;
