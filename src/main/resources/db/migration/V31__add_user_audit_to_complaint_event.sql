ALTER TABLE complaint_event
ADD COLUMN triggered_by_user_id uuid;

ALTER TABLE complaint_event
ADD CONSTRAINT fk_event_user
FOREIGN KEY (triggered_by_user_id)
REFERENCES users(id);

CREATE INDEX idx_event_triggered_user
ON complaint_event(triggered_by_user_id);