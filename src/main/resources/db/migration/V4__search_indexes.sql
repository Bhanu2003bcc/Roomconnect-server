ALTER TABLE listings ADD COLUMN search_vector TSVECTOR
  GENERATED ALWAYS AS (
    to_tsvector('english', coalesce(title,'') || ' ' || coalesce(description,'') || ' ' || coalesce(address_text,''))
  ) STORED;

CREATE INDEX listings_fts_idx ON listings USING GIN(search_vector);
