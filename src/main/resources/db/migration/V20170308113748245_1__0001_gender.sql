CREATE TABLE IF NOT EXISTS gender
(
  id bigint NOT NULL DEFAULT nextval('gender_id_seq'::regclass),
  name VARCHAR(1) UNIQUE,
  description VARCHAR(4000),
  CONSTRAINT gender_pk PRIMARY KEY (id)
)