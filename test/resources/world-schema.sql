CREATE TABLE IF NOT EXISTS city (
    id integer AUTO_INCREMENT PRIMARY KEY,
    name varchar NOT NULL,
    countrycode character(3) NOT NULL,
    district varchar NOT NULL,
    population integer NOT NULL
);

CREATE TABLE IF NOT EXISTS country (
    code character(3) PRIMARY KEY,
    name varchar NOT NULL,
    continent varchar NOT NULL,
    region varchar NOT NULL,
    surfacearea real NOT NULL,
    indepyear smallint,
    population integer NOT NULL,
    lifeexpectancy real,
    gnp numeric(10,2),
    gnpold numeric(10,2),
    localname varchar NOT NULL,
    governmentform varchar NOT NULL,
    headofstate varchar,
    capital integer,
    code2 character(2) NOT NULL
);

CREATE TABLE IF NOT EXISTS countrylanguage (
    countrycode character(3) NOT NULL,
    language varchar NOT NULL,
    isofficial boolean NOT NULL,
    percentage real NOT NULL
);
