DELETE FROM posts;
INSERT INTO  posts (title, content) VALUES ('post one', 'content of post one');

insert into posts (id, title, content)
select x, 'Post ' || x, 'Content ' || x from system_range(10, 110);


DELETE FROM tweets;
insert into tweets (id, title, content)
select x, 'Tweet ' || x, 'Content ' || x from system_range(50, 150);
