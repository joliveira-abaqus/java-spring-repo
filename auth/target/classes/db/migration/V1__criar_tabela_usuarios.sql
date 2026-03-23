CREATE TABLE usuarios (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    nome varchar(100) NOT NULL,
    email varchar(100) NOT NULL,
    senha varchar(255) NOT NULL,
    role varchar(50) NOT NULL,
    ativo boolean NOT NULL DEFAULT true,
    PRIMARY KEY (id),
    UNIQUE KEY uk_usuario_email (email)
);
