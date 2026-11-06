CREATE TABLE usuarios (
    id            BIGSERIAL    PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    rol           VARCHAR(20)  NOT NULL,
    CONSTRAINT ck_usuarios_rol CHECK (rol IN ('USER', 'ADMIN'))
);