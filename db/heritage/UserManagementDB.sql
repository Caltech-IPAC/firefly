
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
 appname varchar(30),
 historytoken varchar(20000) not null,
 description varchar(2000) not null,
 favorite boolean,
 timeadded datetime not null
);

create table tags
(tagid int not null auto_increment primary key,
 tagname varchar(100),
 appname varchar(30),
 historytoken varchar(20000),
 description varchar(2000),
 istag boolean,
 numhits int,
 timecreated datetime not null,
 timeused datetime,
 createdby varchar(100),        -- loginname or userkey from a guest user
 constraint UNQ_tagname unique (tagname)
);




