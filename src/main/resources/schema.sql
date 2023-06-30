-- omegatrack v1

CREATE TABLE "coordinates" (
	id		bigint CONSTRAINT firstkey PRIMARY KEY,
	uuid	text not null,
	world	text not null,
	time	bigint not null,
	x		double precision not null,
	z		double precision not null
);

CREATE TABLE "userprefs" (
	uuid			text not null,
	preferences		text not null
);