-- CloudStream Sync için Supabase tablo yapısı
-- Supabase Dashboard → SQL Editor'de çalıştır

-- 1. Ana tablo
CREATE TABLE IF NOT EXISTS cloudstream_sync (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id TEXT NOT NULL,
  data JSONB NOT NULL,
  timestamp BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  UNIQUE(user_id)
);

-- 2. İndeksler (performans için)
CREATE INDEX IF NOT EXISTS idx_cloudstream_user_id ON cloudstream_sync(user_id);
CREATE INDEX IF NOT EXISTS idx_cloudstream_timestamp ON cloudstream_sync(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_cloudstream_updated_at ON cloudstream_sync(updated_at DESC);

-- 3. Otomatik updated_at güncelleme
CREATE OR REPLACE FUNCTION update_cloudstream_sync_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_cloudstream_sync_updated_at
  BEFORE UPDATE ON cloudstream_sync
  FOR EACH ROW
  EXECUTE FUNCTION update_cloudstream_sync_updated_at();

-- 4. Row Level Security (RLS) - Opsiyonel ama önerilen
ALTER TABLE cloudstream_sync ENABLE ROW LEVEL SECURITY;

-- Herkes kendi verisini okuyabilir/yazabilir (anon key ile)
CREATE POLICY "Herkes kendi verisini yönetebilir"
  ON cloudstream_sync
  FOR ALL
  USING (true)
  WITH CHECK (true);

-- 5. Eski kayıtları temizleme (opsiyonel - 90 gün sonra sil)
-- Sadece istersen aktifleştir, yoksa yorum satırı bırak
-- CREATE EXTENSION IF NOT EXISTS pg_cron;
-- 
-- SELECT cron.schedule(
--   'cloudstream-sync-cleanup',
--   '0 3 * * *', -- Her gün saat 03:00'te
--   $$DELETE FROM cloudstream_sync WHERE updated_at < NOW() - INTERVAL '90 days'$$
-- );

-- 6. Veri yapısı örneği (JSONB içeriği)
/*
{
  "bookmarks": ["movie_id_1", "movie_id_2"],
  "watch_positions": {
    "movie_id_1": 12345,
    "series_s1e1": 67890
  },
  "search_history": ["action movies", "anime"],
  "extensions": ["DiziPal", "HDFilmCehennemi"],
  "settings": {
    "theme": "dark",
    "language": "tr",
    "autoplay": "true"
  },
  "timestamp": 1726513042181
}
*/

-- 7. Kontrol: Tablo başarıyla oluşturuldu mu?
SELECT 
  table_name, 
  column_name, 
  data_type 
FROM information_schema.columns 
WHERE table_name = 'cloudstream_sync' 
ORDER BY ordinal_position;

-- 8. Test verisi ekle (opsiyonel - test için)
-- INSERT INTO cloudstream_sync (user_id, data) VALUES (
--   'test_user_123',
--   '{
--     "bookmarks": ["test_movie"],
--     "watch_positions": {"test_movie": 1000},
--     "search_history": ["test"],
--     "extensions": [],
--     "settings": {},
--     "timestamp": 1726513042181
--   }'::JSONB
-- );

-- 9. Test verisini kontrol et
-- SELECT * FROM cloudstream_sync WHERE user_id = 'test_user_123';

-- 10. Test verisini sil
-- DELETE FROM cloudstream_sync WHERE user_id = 'test_user_123';
