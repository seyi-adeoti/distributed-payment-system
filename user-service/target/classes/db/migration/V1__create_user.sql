CREATE TABLE "users"
(
    id          UUID         NOT NULL,
    first_name  VARCHAR(255) NOT NULL,
    last_name   VARCHAR(255) NOT NULL,
    middle_name VARCHAR(255),
    user_name   VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITHOUT TIME ZONE,
    dob         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    role        VARCHAR(255),
    password    VARCHAR(255),
    is_enabled  BOOLEAN      NOT NULL,
    CONSTRAINT pk_user PRIMARY KEY (id)
);