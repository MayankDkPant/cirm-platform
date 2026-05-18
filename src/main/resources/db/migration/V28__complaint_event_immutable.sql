-- -----------------------------------------------------------------------------
-- complaint_event immutability guard
-- -----------------------------------------------------------------------------
-- complaint_event is an append-only audit table.
-- Any UPDATE or DELETE operation must be blocked at database level
-- to preserve audit integrity.

CREATE OR REPLACE FUNCTION enforce_complaint_event_immutable()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        RAISE EXCEPTION 'complaint_event is immutable: UPDATE is not allowed';
    ELSIF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'complaint_event is immutable: DELETE is not allowed';
    END IF;

    RETURN OLD;
END;
$$;

-- Recreate trigger safely so migration can be reapplied in non-prod environments.
DROP TRIGGER IF EXISTS trg_complaint_event_immutable ON complaint_event;

CREATE TRIGGER trg_complaint_event_immutable
BEFORE UPDATE OR DELETE ON complaint_event
FOR EACH ROW
EXECUTE FUNCTION enforce_complaint_event_immutable();
