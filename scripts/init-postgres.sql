-- Create databases for each write-side service
CREATE DATABASE user_db;
CREATE DATABASE company_db;
CREATE DATABASE officer_db;

-- Grant all privileges to the admin user on each database
GRANT ALL PRIVILEGES ON DATABASE user_db TO admin;
GRANT ALL PRIVILEGES ON DATABASE company_db TO admin;
GRANT ALL PRIVILEGES ON DATABASE officer_db TO admin;
