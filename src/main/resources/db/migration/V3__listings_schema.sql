CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE listings (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  city_id INT NOT NULL DEFAULT 1,
  category VARCHAR(30) NOT NULL CHECK (category IN ('pg','1bhk','2bhk','3bhk','independent_room')),
  title VARCHAR(255) NOT NULL,
  description TEXT,
  rent_amount NUMERIC(10,2) NOT NULL,
  deposit_amount NUMERIC(10,2),
  bathroom_type VARCHAR(20) CHECK (bathroom_type IN ('shared','attached')),
  furnishing VARCHAR(30),
  gender_preference VARCHAR(20) NOT NULL DEFAULT 'any' CHECK (gender_preference IN ('any','male','female')),
  food_included BOOLEAN NOT NULL DEFAULT FALSE,
  food_type VARCHAR(20) CHECK (food_type IN ('veg','non-veg','both','none')),
  curfew_time TIME,
  ac VARCHAR(10) CHECK (ac IN ('ac','non-ac')),
  wifi BOOLEAN NOT NULL DEFAULT FALSE,
  parking BOOLEAN NOT NULL DEFAULT FALSE,
  laundry BOOLEAN NOT NULL DEFAULT FALSE,
  address_text TEXT NOT NULL,
  latitude DOUBLE PRECISION NOT NULL,
  longitude DOUBLE PRECISION NOT NULL,
  geo GEOGRAPHY(Point, 4326) GENERATED ALWAYS AS (ST_MakePoint(longitude, latitude)) STORED,
  status VARCHAR(30) NOT NULL DEFAULT 'available' CHECK (status IN ('available','occupied','available_from')),
  available_from_date DATE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX listings_geo_idx ON listings USING GIST(geo);
CREATE INDEX listings_city_status_idx ON listings(city_id, status);
CREATE INDEX listings_owner_idx ON listings(owner_id);
CREATE INDEX listings_gender_idx ON listings(gender_preference);

CREATE TABLE listing_media (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  listing_id UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
  type VARCHAR(10) CHECK (type IN ('photo','video')),
  cdn_url TEXT NOT NULL,
  thumbnail_url TEXT,
  sort_order INT NOT NULL DEFAULT 0,
  processing_status VARCHAR(20) NOT NULL DEFAULT 'pending' CHECK (processing_status IN ('pending','done','failed')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
