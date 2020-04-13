# some selects
SELECT * FROM user;
SELECT * FROM search;
SELECT * FROM search_param;
SELECT * FROM task;
SELECT * FROM result;
SELECT * FROM result_param;

SELECT s.id, p.name, p.value
FROM search as s, search_param as p
WHERE s.active = true AND s.id = p.search_id;
