-- Dittofeed Power Parity: Workflows, Traits, and Broadcasts

CREATE TABLE user_traits (
    email VARCHAR(255) PRIMARY KEY REFERENCES users(email),
    traits JSONB DEFAULT '{}'::jsonb,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE marketing_workflows (
    workflow_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    trigger_event VARCHAR(50) NOT NULL, -- e.g., 'USER_REGISTERED', 'CART_ABANDONED'
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE marketing_workflow_steps (
    step_id SERIAL PRIMARY KEY,
    workflow_id INT REFERENCES marketing_workflows(workflow_id),
    sequence_order INT NOT NULL,
    step_type VARCHAR(20) NOT NULL, -- 'SEND_EMAIL', 'SEND_WHATSAPP', 'WAIT', 'CONDITION'
    config JSONB NOT NULL, -- e.g., {"template": "welcome", "delay_hours": 24, "condition": "not_purchased"}
    UNIQUE(workflow_id, sequence_order)
);

CREATE TABLE workflow_executions (
    execution_id BIGSERIAL PRIMARY KEY,
    workflow_id INT REFERENCES marketing_workflows(workflow_id),
    user_email VARCHAR(255) NOT NULL,
    current_step_order INT NOT NULL,
    next_run_at TIMESTAMP NOT NULL,
    status VARCHAR(20) DEFAULT 'IN_PROGRESS', -- 'IN_PROGRESS', 'COMPLETED', 'FAILED'
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE TABLE marketing_broadcasts (
    broadcast_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    segment_name VARCHAR(50) NOT NULL, -- 'ALL', 'VIP', 'CHURN_RISK'
    template_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) DEFAULT 'DRAFT', -- 'DRAFT', 'SENDING', 'SENT'
    sent_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP
);
