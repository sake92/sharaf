CREATE TABLE sharaf_session (
  session_id VARCHAR(128) PRIMARY KEY,
  created_at BIGINT NOT NULL,
  last_accessed_at BIGINT NOT NULL,
  session_data TEXT NOT NULL
);

CREATE INDEX sharaf_session_last_accessed_at_idx ON sharaf_session (last_accessed_at);
