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
  dob timestamp without time zone,
  join_date timestamp without time zone,
  active_date timestamp without time zone,
  deactived_date timestamp without time zone,
  created_date timestamp without time zone DEFAULT now(),
   CONSTRAINT users_gender_fk FOREIGN KEY (gender) REFERENCES gender(name),
   CONSTRAINT user_status_fk FOREIGN KEY (status) REFERENCES user_statuses(status)
)