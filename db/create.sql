# create db
DROP DATABASE searches;
CREATE DATABASE searches;
USE searches;

CREATE TABLE search (
    id integer unsigned unique PRIMARY KEY AUTO_INCREMENT,
    user_id integer unsigned NOT NULL,
    brand varchar(255) NOT NULL,
    model varchar(255) NULL,
    min_mileage int unsigned NULL,
    max_mileage int unsigned NULL,
    active bit NOT NULL DEFAULT true
);

CREATE TABLE task (
    id varchar(36) unique NOT NULL,
    search_id integer unsigned NOT NULL,
    start_time datetime NOT NULL,
    end_time datetime NULL,

    PRIMARY KEY (id),
    FOREIGN KEY (search_id) REFERENCES search(id)
);

CREATE TABLE result (
  task_id varchar(36) NOT NULL,
  name varchar(256),
  link text,

  FOREIGN KEY (task_id) REFERENCES task(id)
);

CREATE TABLE user (
  id integer unsigned unique PRIMARY KEY AUTO_INCREMENT,
  login varchar(255) unique NOT NULL
);

# sample data
INSERT INTO user (login) VALUES ('student_1');
INSERT INTO user (login) VALUES ('student_2');
INSERT INTO user (login) VALUES ('student_3');

INSERT INTO search (user_id, brand, model, min_mileage, max_mileage)
VALUES ((SELECT id FROM user WHERE login='student_1'), 'opel', 'astra', 10000, 100000);
INSERT INTO search (user_id, brand, model, min_mileage, max_mileage)
VALUES ((SELECT id FROM user WHERE login='student_1'), 'skoda', 'fabia', 10000, 100000);
INSERT INTO search (user_id, brand, model, min_mileage, max_mileage)
VALUES ((SELECT id FROM user WHERE login='student_2'), 'tesla', 'Model X', 10000, 100000);
INSERT INTO search (user_id, brand, model, min_mileage, max_mileage)
VALUES ((SELECT id FROM user WHERE login='student_2'), 'audi', null, null, 250000);

SELECT * FROM search;
SELECT * FROM task;
SELECT * FROM result;

