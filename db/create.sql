# create db
DROP DATABASE IF EXISTS searches;
CREATE DATABASE searches;
USE searches;

CREATE TABLE search (
    id integer unsigned unique PRIMARY KEY AUTO_INCREMENT,
    user_id integer unsigned NOT NULL,
    active bit NOT NULL DEFAULT true
);

CREATE TABLE search_param (
    search_id integer unsigned NOT NULL,
    name varchar(255) NOT NULL,
    value varchar(255) NOT NULL,

    FOREIGN KEY (search_id) REFERENCES search(id)
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
    id bigint unsigned unique KEY AUTO_INCREMENT,
    task_id varchar(36) NOT NULL,
    title varchar(256) NOT NULL,
    subtitle text NULL,
    url text NULL,
    imgUrl text NULL,

    PRIMARY KEY (id),
    FOREIGN KEY (task_id) REFERENCES task(id)
);


CREATE TABLE result_param (
    result_id bigint unsigned NOT NULL,
    name varchar(255) NOT NULL,
    value varchar(255) NOT NULL,

    FOREIGN KEY (result_id) REFERENCES result(id)
);


CREATE TABLE user (
  id integer unsigned unique PRIMARY KEY AUTO_INCREMENT,
  login varchar(255) unique NOT NULL
);

# sample data
INSERT INTO user (login) VALUES ('student_1');
INSERT INTO user (login) VALUES ('student_2');
INSERT INTO user (login) VALUES ('student_3');

SELECT id INTO @user1id FROM user WHERE login='student_1';
SELECT id INTO @user2id FROM user WHERE login='student_2';
SELECT id INTO @user3id FROM user WHERE login='student_3';

# searches
INSERT INTO search (user_id) VALUES (@user1id);
INSERT INTO search (user_id) VALUES (@user2id);
INSERT INTO search (user_id, active) VALUES (@user3id, false);

SELECT id INTO @user1search FROM search WHERE user_id=@user1id;
SELECT id INTO @user2search FROM search WHERE user_id=@user2id;
SELECT id INTO @user3search FROM search WHERE user_id=@user3id;

# params
INSERT INTO search_param (search_id, name, value)
VALUES (@user1search, 'brand', 'opel');
INSERT INTO search_param (search_id, name, value)
VALUES (@user1search, 'model', 'astra');
INSERT INTO search_param (search_id, name, value)
VALUES (@user1search, 'maxMileage', '150000');
INSERT INTO search_param (search_id, name, value)
VALUES (@user1search, 'minMileage', '10000');
INSERT INTO search_param (search_id, name, value)
VALUES (@user1search, 'minYear', '2010');
INSERT INTO search_param (search_id, name, value)
VALUES (@user1search, 'maxYear', '2016');
INSERT INTO search_param (search_id, name, value)
VALUES (@user1search, 'minEngineSize', '1000');
INSERT INTO search_param (search_id, name, value)
VALUES (@user1search, 'maxEngineSize', '1500');
INSERT INTO search_param (search_id, name, value)
VALUES (@user1search, 'minPrice', '1000');
INSERT INTO search_param (search_id, name, value)
VALUES (@user1search, 'maxPrice', '75000');
INSERT INTO search_param (search_id, name, value)
VALUES (@user1search, 'fuel', 'petrol');

INSERT INTO search_param (search_id, name, value)
VALUES (@user2search, 'brand', 'mercedes-benz');
INSERT INTO search_param (search_id, name, value)
VALUES (@user2search, 'model', 'gls');
INSERT INTO search_param (search_id, name, value)
VALUES (@user2search, 'maxMileage', '100000');
INSERT INTO search_param (search_id, name, value)
VALUES (@user2search, 'minYear', '2015');


INSERT INTO search_param (search_id, name, value)
VALUES (@user3search, 'brand', 'tesla');
INSERT INTO search_param (search_id, name, value)
VALUES (@user3search, 'model', 'model 3');
INSERT INTO search_param (search_id, name, value)
VALUES (@user3search, 'maxMileage', '10');
INSERT INTO search_param (search_id, name, value)
VALUES (@user3search, 'minYear', '2018');
