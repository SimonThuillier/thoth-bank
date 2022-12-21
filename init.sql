ALTER SYSTEM SET max_connections = 50;
ALTER SYSTEM RESET shared_buffers;
CREATE USER thoth_bank WITH PASSWORD 'thoth_bank';

CREATE DATABASE thoth_bank_event_store;
GRANT ALL PRIVILEGES ON DATABASE "thoth_bank_event_store" to thoth_bank;

CREATE DATABASE thoth_bank_projection;
GRANT ALL PRIVILEGES ON DATABASE "thoth_bank_projection" to thoth_bank;