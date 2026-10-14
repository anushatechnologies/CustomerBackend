-- --------------------------------------------------------
-- V28: Seller Support Tickets (separate from customer support_tickets)
-- --------------------------------------------------------

CREATE TABLE IF NOT EXISTS seller_support_tickets (
    ticket_id    INT AUTO_INCREMENT PRIMARY KEY,
    ticket_number VARCHAR(255) NOT NULL UNIQUE,
    seller_id    INT NOT NULL,
    subject      VARCHAR(255) NOT NULL,
    category     VARCHAR(50)  DEFAULT 'GENERAL',
    priority     VARCHAR(50)  DEFAULT 'MEDIUM',
    status       VARCHAR(50)  DEFAULT 'OPEN',
    order_id     INT,
    created_at   DATETIME     NOT NULL,
    updated_at   DATETIME,
    CONSTRAINT fk_seller_support_tickets_seller FOREIGN KEY (seller_id) REFERENCES sellers (seller_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Seller Ticket Messages Table
-- --------------------------------------------------------

CREATE TABLE IF NOT EXISTS seller_ticket_messages (
    message_id     INT AUTO_INCREMENT PRIMARY KEY,
    ticket_id      INT          NOT NULL,
    sender_id      INT          NOT NULL,
    sender_role    VARCHAR(50)  NOT NULL,
    sender_name    VARCHAR(255),
    content        TEXT         NOT NULL,
    attachment_url TEXT,
    timestamp      DATETIME     NOT NULL,
    CONSTRAINT fk_seller_ticket_messages_ticket FOREIGN KEY (ticket_id) REFERENCES seller_support_tickets (ticket_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
