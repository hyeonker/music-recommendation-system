create table if not exists songs (
                                     id bigserial primary key,
                                     title  varchar(200) not null,
    artist varchar(200) not null
    );

create index if not exists idx_songs_id_desc on songs (id desc);
