drop table users;
drop table preferences;
drop table queryhistory;

create table preferences 
(prefid int not null auto_increment primary key,
 loginname varchar(30) not null,
 prefname varchar(180) not null, 
 prefvalue varchar(180), 
 constraint UNQ_Preferences unique (loginname, prefname)
);

-- the maximum length of a URL in Internet Explorer
-- is 2,083 characters, with no more than 2,048 characters 
-- in the path portion of the URL
create table queryhistory
(queryid int not null auto_increment primary key,
 loginname varchar(30) not null,
 historytoken varchar(20000) not null,
 description varchar(2000) not null,
 favorite boolean,
 timeadded datetime not null
);

