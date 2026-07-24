ALTER TABLE conversations DROP CONSTRAINT IF EXISTS conversations_listing_id_fkey;

ALTER TABLE conversations ALTER COLUMN listing_id DROP NOT NULL;

ALTER TABLE conversations ADD CONSTRAINT conversations_listing_id_fkey 
  FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE SET NULL;

ALTER TABLE conversations ADD COLUMN IF NOT EXISTS listing_title VARCHAR(255);
ALTER TABLE conversations ADD COLUMN IF NOT EXISTS listing_address TEXT;
ALTER TABLE conversations ADD COLUMN IF NOT EXISTS listing_rent NUMERIC(12,2);

-- Backfill snapshot details for existing conversations from listings
UPDATE conversations c
SET listing_title = l.title,
    listing_address = l.address_text,
    listing_rent = l.rent_amount
FROM listings l
WHERE c.listing_id = l.id
  AND c.listing_title IS NULL;
