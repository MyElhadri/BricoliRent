-- =========================================================
-- Jeux de données initiaux
-- =========================================================

-- Users
INSERT INTO users (full_name, email, password_hash, active)
VALUES
    ('Admin Principal', 'admin@bricolirent.com', 'admin123hash', TRUE),
    ('Client Test', 'client@bricolirent.com', 'client123hash', TRUE),
    ('Agent Test', 'agent@bricolirent.com', 'agent123hash', TRUE);

-- Héritage
INSERT INTO admins (user_id) VALUES (1);
INSERT INTO clients (user_id, score) VALUES (2, 100);
INSERT INTO agents (user_id, employee_code) VALUES (3, 'AG001');

-- Categories
INSERT INTO categories (name, description)
VALUES
    ('Perçage', 'Outils pour percer différents matériaux'),
    ('Jardinage', 'Outils destinés au jardinage');

-- Tools
INSERT INTO tools (
    category_id, name, description, price_per_day, deposit_amount,
    total_quantity, available_quantity, active
)
VALUES
    (1, 'Perceuse Bosch', 'Perceuse électrique professionnelle', 120.00, 500.00, 10, 10, TRUE),
    (1, 'Marteau perforateur', 'Outil pour béton et maçonnerie', 180.00, 700.00, 5, 5, TRUE),
    (2, 'Taille-haie', 'Taille-haie électrique', 90.00, 300.00, 4, 4, TRUE);

-- Reservation exemple
INSERT INTO reservations (
    client_id, tool_id, handled_by_agent_id, checkout_agent_id,
    quantity, start_date, end_date, status,
    estimated_rental_amount, estimated_deposit_amount,
    approved_automatically, approval_reason, approved_at, checked_out_at, receipt_number
)
VALUES (
           2, 1, 3, NULL,
           2, '2026-03-26', '2026-03-28', 'PENDING',
           240.00, 1000.00,
           FALSE, 'En attente de validation agent', NULL, NULL, NULL
       );