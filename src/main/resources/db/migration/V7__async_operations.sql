CREATE TABLE async_operations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(50) NOT NULL,
    reference_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'QUEUED',
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_async_ops_ref_type ON async_operations(reference_id, type);
