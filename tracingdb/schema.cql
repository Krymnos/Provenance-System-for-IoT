CREATE KEYSPACE IF NOT EXISTS provenance WITH replication {'class':'SimpleStrategy','replication_factor':'1'};

CREATE TABLE IF NOT EXISTS provenance.provenanceTable (
  ID text PRIMARY KEY,
  IID map<text,text>,
  LOCATION text,
  LINE bigint,
  CLASS text,
  APPLICATION text,
  CREATE_TIME timestamp,
  SEND_TIME timestamp,
  RECEIVE_TIME timestamp,
  LAT double,
  LONG double
);
