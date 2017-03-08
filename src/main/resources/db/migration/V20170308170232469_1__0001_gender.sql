CREATE TABLE IF NOT EXISTS gender
(
  id SMALLSERIAL PRIMARY KEY NOT NULL,
  name VARCHAR(1) UNIQUE,
  description VARCHAR(4000)
);

INSERT INTO gender(name, description) VALUES ('M', 'Male');
INSERT INTO gender(name, description) VALUES ('F', 'Female');