DROP TABLE IF EXISTS gender CASCADE;
CREATE TABLE IF NOT EXISTS gender
(
 -- id SMALLSERIAL PRIMARY KEY NOT NULL,
  name VARCHAR(50) PRIMARY KEY NOT NULL,
  description VARCHAR(4000),
  columnPlane VARCHAR(80)
);

INSERT INTO gender(name, description) VALUES ('M', 'Male');
INSERT INTO gender(name, description) VALUES ('F', 'Female');