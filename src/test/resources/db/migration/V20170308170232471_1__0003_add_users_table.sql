DROP TABLE IF EXISTS users CASCADE;
CREATE TABLE IF NOT EXISTS users
(
  id BIGSERIAL NOT NULL PRIMARY KEY,
  company_id bigint NOT NULL,
  email VARCHAR(300),
  username VARCHAR(300),
  password VARCHAR(4000),
  first_name VARCHAR(400),
  last_name VARCHAR(400),
  gender VARCHAR(50),
  status VARCHAR(50),
  dob DATE,
  join_date DATE,
  active_date DATE,
  deactived_date DATE,
  created_date DATE DEFAULT now(),
   CONSTRAINT users_gender_fk FOREIGN KEY (gender) REFERENCES gender(name),
   CONSTRAINT user_status_fk FOREIGN KEY (status) REFERENCES user_statuses(status)
)