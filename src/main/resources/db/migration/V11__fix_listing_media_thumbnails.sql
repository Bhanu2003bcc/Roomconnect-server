-- Fix existing listing_media rows where thumbnail_url was set to non-existent _thumb file keys
UPDATE listing_media
SET thumbnail_url = cdn_url
WHERE thumbnail_url LIKE '%_thumb%';
