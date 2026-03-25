-- =========================================================
-- BricoliRent - Base de données PostgreSQL
-- Version basée sur l’UML fourni
-- =========================================================

-- Nettoyage (optionnel pour les tests)
DROP TABLE IF EXISTS payments CASCADE;
DROP TABLE IF EXISTS return_records CASCADE;
DROP TABLE IF EXISTS reservations CASCADE;
DROP TABLE IF EXISTS tools CASCADE;
DROP TABLE IF EXISTS categories CASCADE;
DROP TABLE IF EXISTS agents CASCADE;
DROP TABLE IF EXISTS clients CASCADE;
DROP TABLE IF EXISTS admins CASCADE;
DROP TABLE IF EXISTS users CASCADE;

DROP TYPE IF EXISTS reservation_status CASCADE;
DROP TYPE IF EXISTS payment_type CASCADE;
DROP TYPE IF EXISTS payment_status CASCADE;
DROP TYPE IF EXISTS payment_method CASCADE;

-- =========================================================
-- 1) Types ENUM
-- =========================================================

CREATE TYPE reservation_status AS ENUM (
    'PENDING',
    'APPROVED',
    'REJECTED',
    'CHECKED_OUT',
    'RETURNED'
);

CREATE TYPE payment_type AS ENUM (
    'RENTAL',
    'DEPOSIT',
    'LATE_PENALTY',
    'REFUND'
);

CREATE TYPE payment_status AS ENUM (
    'PENDING',
    'PAID'
);

CREATE TYPE payment_method AS ENUM (
    'CASH'
);

-- =========================================================
-- 2) Table mère : users
-- =========================================================

CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       full_name VARCHAR(150) NOT NULL,
                       email VARCHAR(150) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       active BOOLEAN NOT NULL DEFAULT TRUE
);

-- =========================================================
-- 3) Spécialisation : admin
-- =========================================================

CREATE TABLE admins (
                        user_id BIGINT PRIMARY KEY,
                        CONSTRAINT fk_admin_user
                            FOREIGN KEY (user_id)
                                REFERENCES users(id)
                                ON DELETE CASCADE
);

-- =========================================================
-- 4) Spécialisation : client
-- =========================================================

CREATE TABLE clients (
                         user_id BIGINT PRIMARY KEY,
                         score INTEGER NOT NULL DEFAULT 0,
                         CONSTRAINT fk_client_user
                             FOREIGN KEY (user_id)
                                 REFERENCES users(id)
                                 ON DELETE CASCADE
);

-- =========================================================
-- 5) Spécialisation : agent
-- =========================================================

CREATE TABLE agents (
                        user_id BIGINT PRIMARY KEY,
                        employee_code VARCHAR(100) NOT NULL UNIQUE,
                        CONSTRAINT fk_agent_user
                            FOREIGN KEY (user_id)
                                REFERENCES users(id)
                                ON DELETE CASCADE
);

-- =========================================================
-- 6) Categories
-- =========================================================

CREATE TABLE categories (
                            id BIGSERIAL PRIMARY KEY,
                            name VARCHAR(100) NOT NULL UNIQUE,
                            description TEXT
);

-- =========================================================
-- 7) Tools
-- =========================================================

CREATE TABLE tools (
                       id BIGSERIAL PRIMARY KEY,
                       category_id BIGINT NOT NULL,
                       name VARCHAR(150) NOT NULL,
                       description TEXT,
                       price_per_day NUMERIC(10,2) NOT NULL CHECK (price_per_day >= 0),
                       deposit_amount NUMERIC(10,2) NOT NULL CHECK (deposit_amount >= 0),
                       total_quantity INTEGER NOT NULL CHECK (total_quantity >= 0),
                       available_quantity INTEGER NOT NULL CHECK (available_quantity >= 0),
                       active BOOLEAN NOT NULL DEFAULT TRUE,

                       CONSTRAINT fk_tool_category
                           FOREIGN KEY (category_id)
                               REFERENCES categories(id)
                               ON DELETE RESTRICT,

                       CONSTRAINT chk_tool_available_quantity
                           CHECK (available_quantity <= total_quantity)
);

-- =========================================================
-- 8) Reservations
-- =========================================================
-- D’après l’UML :
-- - un client fait plusieurs réservations
-- - une réservation concerne un tool
-- - un agent peut gérer / checkout
-- - certains champs métier sont stockés directement ici

CREATE TABLE reservations (
                              id BIGSERIAL PRIMARY KEY,

                              client_id BIGINT NOT NULL,
                              tool_id BIGINT NOT NULL,

                              handled_by_agent_id BIGINT NULL,
                              checkout_agent_id BIGINT NULL,

                              quantity INTEGER NOT NULL CHECK (quantity > 0),

                              start_date DATE NOT NULL,
                              end_date DATE NOT NULL,
                              reservation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              status reservation_status NOT NULL DEFAULT 'PENDING',

                              estimated_rental_amount NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (estimated_rental_amount >= 0),
                              estimated_deposit_amount NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (estimated_deposit_amount >= 0),

                              approved_automatically BOOLEAN NOT NULL DEFAULT FALSE,
                              approval_reason TEXT,
                              approved_at TIMESTAMP NULL,
                              checked_out_at TIMESTAMP NULL,
                              receipt_number VARCHAR(100),

                              CONSTRAINT fk_reservation_client
                                  FOREIGN KEY (client_id)
                                      REFERENCES clients(user_id)
                                      ON DELETE RESTRICT,

                              CONSTRAINT fk_reservation_tool
                                  FOREIGN KEY (tool_id)
                                      REFERENCES tools(id)
                                      ON DELETE RESTRICT,

                              CONSTRAINT fk_reservation_handled_agent
                                  FOREIGN KEY (handled_by_agent_id)
                                      REFERENCES agents(user_id)
                                      ON DELETE SET NULL,

                              CONSTRAINT fk_reservation_checkout_agent
                                  FOREIGN KEY (checkout_agent_id)
                                      REFERENCES agents(user_id)
                                      ON DELETE SET NULL,

                              CONSTRAINT chk_reservation_dates
                                  CHECK (end_date >= start_date)
);

-- =========================================================
-- 9) ReturnRecord
-- =========================================================
-- D’après l’UML :
-- - une réservation peut avoir 0 ou 1 return_record
-- - un agent peut gérer plusieurs return_records

CREATE TABLE return_records (
                                id BIGSERIAL PRIMARY KEY,

                                reservation_id BIGINT NOT NULL UNIQUE,
                                handled_by_agent_id BIGINT NULL,

                                actual_return_date TIMESTAMP NOT NULL,
                                late_days INTEGER NOT NULL DEFAULT 0 CHECK (late_days >= 0),
                                late_penalty NUMERIC(10,2) NOT NULL DEFAULT 0 CHECK (late_penalty >= 0),
                                notes TEXT,

                                CONSTRAINT fk_return_record_reservation
                                    FOREIGN KEY (reservation_id)
                                        REFERENCES reservations(id)
                                        ON DELETE CASCADE,

                                CONSTRAINT fk_return_record_agent
                                    FOREIGN KEY (handled_by_agent_id)
                                        REFERENCES agents(user_id)
                                        ON DELETE SET NULL
);

-- =========================================================
-- 10) Payments
-- =========================================================
-- D’après l’UML :
-- - une réservation peut avoir plusieurs paiements
-- - un agent peut enregistrer plusieurs paiements

CREATE TABLE payments (
                          id BIGSERIAL PRIMARY KEY,

                          reservation_id BIGINT NOT NULL,
                          recorded_by_agent_id BIGINT NULL,

                          type payment_type NOT NULL,
                          method payment_method NOT NULL,
                          amount NUMERIC(10,2) NOT NULL CHECK (amount >= 0),
                          status payment_status NOT NULL DEFAULT 'PENDING',
                          payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          receipt_number VARCHAR(100),
                          notes TEXT,

                          CONSTRAINT fk_payment_reservation
                              FOREIGN KEY (reservation_id)
                                  REFERENCES reservations(id)
                                  ON DELETE CASCADE,

                          CONSTRAINT fk_payment_agent
                              FOREIGN KEY (recorded_by_agent_id)
                                  REFERENCES agents(user_id)
                                  ON DELETE SET NULL
);

-- =========================================================
-- 11) Index utiles
-- =========================================================

CREATE INDEX idx_tools_category_id ON tools(category_id);

CREATE INDEX idx_reservations_client_id ON reservations(client_id);
CREATE INDEX idx_reservations_tool_id ON reservations(tool_id);
CREATE INDEX idx_reservations_handled_by_agent_id ON reservations(handled_by_agent_id);
CREATE INDEX idx_reservations_checkout_agent_id ON reservations(checkout_agent_id);
CREATE INDEX idx_reservations_status ON reservations(status);
CREATE INDEX idx_reservations_start_date ON reservations(start_date);
CREATE INDEX idx_reservations_end_date ON reservations(end_date);

CREATE INDEX idx_return_records_agent_id ON return_records(handled_by_agent_id);

CREATE INDEX idx_payments_reservation_id ON payments(reservation_id);
CREATE INDEX idx_payments_agent_id ON payments(recorded_by_agent_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_type ON payments(type);