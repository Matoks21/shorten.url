 create table roles (
        id bigint generated by default as identity,
        name varchar(20) unique check (name in ('ROLE_USER','ROLE_ADMIN')),
        primary key (id)
    );

 create table users (
        id bigint generated by default as identity,
        username varchar(50) not null unique,
        password varchar(255) not null,
        email varchar(50) not null unique,
        primary key (id)
    );

    create table user_roles (
        role_id bigint not null,
        user_id bigint not null,
        primary key (role_id, user_id)
    );

