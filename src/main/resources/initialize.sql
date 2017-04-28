CREATE TABLE IF NOT EXISTS book (
  turn varchar(4) not null primary key,
  x int not null,
  y int not null,
  next varchar(4));

-- add book entries