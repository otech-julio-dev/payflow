CREATE DATABASE IF NOT EXISTS payflow_auth;
CREATE DATABASE IF NOT EXISTS payflow_wallets;
CREATE DATABASE IF NOT EXISTS payflow_transfers;

GRANT ALL PRIVILEGES ON payflow_auth.*      TO 'payflow_admin'@'%';
GRANT ALL PRIVILEGES ON payflow_wallets.*   TO 'payflow_admin'@'%';
GRANT ALL PRIVILEGES ON payflow_transfers.* TO 'payflow_admin'@'%';
FLUSH PRIVILEGES;
