DROP TABLE IF EXISTS user_telephones CASCADE;
CREATE TABLE  IF NOT EXISTS user_telephones(
    user_id BIGSERIAL NOT NULL ,
    telephone_id  BIGSERIAL NOT NULL,
    CONSTRAINT user_telephones_pk PRIMARY KEY (user_id, telephone_id),
    CONSTRAINT users_fk FOREIGN KEY (user_id) REFERENCES users(id)
  )