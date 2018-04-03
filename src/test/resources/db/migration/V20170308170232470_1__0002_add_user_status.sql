DROP TABLE IF EXISTS user_statuses CASCADE;
  CREATE TABLE  IF NOT EXISTS user_statuses(
    status VARCHAR(50) PRIMARY KEY ,
    description  VARCHAR(500)
  );

  INSERT INTO user_statuses(status) VALUES ('ACTIVE');
  INSERT INTO user_statuses(status) VALUES ('DEACTIVATED');


