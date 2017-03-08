CREATE TABLE IF NOT EXISTS users
(
  id bigint NOT NULL DEFAULT nextval('users_id_seq'::regclass),
  company_id bigint NOT NULL,
  email character varying(300),
  username character varying(300),
  password character varying(4000),
  first_name character varying(400),
  last_name character varying(400),
  gender character varying(1),
  dob timestamp without time zone,
  join_date timestamp without time zone,
  active_date timestamp without time zone,
  deactived_date timestamp without time zone,
  created_date timestamp without time zone DEFAULT now(),
  CONSTRAINT users_pkey PRIMARY KEY (id),
  CONSTRAINT users_gender_fk FOREIGN KEY (gender) REFERENCES gender(name)
)