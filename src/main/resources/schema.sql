-- omegatrack v2

CREATE SEQUENCE coordinates_id_seq;

CREATE TABLE "coordinates" (
	id		bigint CONSTRAINT firstkey PRIMARY KEY DEFAULT nextval('coordinates_id_seq'),
	uuid	text not null,
	world	text not null,
	time	bigint not null,
	x		double precision not null,
	z		double precision not null
);

ALTER SEQUENCE coordinates_id_seq OWNED BY coordinates.id;

CREATE TABLE "userprefs" (
	uuid			text not null,
	preferences		text not null
);

GRANT USAGE, SELECT ON SEQUENCE coordinates_id_seq TO alexandria;