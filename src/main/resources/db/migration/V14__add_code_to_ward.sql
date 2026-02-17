DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name='ward'
        AND column_name='Code__c'
    ) THEN
        ALTER TABLE ward ADD COLUMN "Code__c" VARCHAR(50);
    END IF;
END $$;

-- ensure NOT NULL
ALTER TABLE ward
ALTER COLUMN "Code__c" SET NOT NULL;

-- ensure unique index exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE indexname = 'ux_ward_code'
    ) THEN
        CREATE UNIQUE INDEX ux_ward_code ON ward("Code__c");
    END IF;
END $$;
