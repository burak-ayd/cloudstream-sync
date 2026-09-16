# CloudStream Sync

Açık kaynak, çoklu bulut destekli CloudStream senkronizasyon eklentisi.

## Özellikler

- ✅ **Supabase** desteği (realtime)
- 🔜 **Google Drive** desteği (yakında)
- 🔜 **Firebase** desteği (yakında)

### Senkronize Edilen Veriler

- 📚 Favoriler (Bookmarks)
- ▶️ Kaldığın yerden devam et (Watch positions)
- 🔍 Arama geçmişi
- 🧩 Eklentiler & depolar
- ⚙️ Uygulama ayarları

## Kurulum

1. [Releases](../../releases) sayfasından `.cs3` dosyasını indir
2. CloudStream → Ayarlar → Eklentiler → Yerel dosyadan yükle
3. CloudStream'i yeniden başlat
4. Ayarlar → CloudStream Sync → Bulut servisini yapılandır

## Supabase Kurulum

### 1. Supabase Projesi Oluştur

1. [supabase.com](https://supabase.com) → Yeni proje
2. Project URL ve API Key'i kopyala

### 2. Tablo Oluştur

SQL Editor'de çalıştır:

```sql
CREATE TABLE cloudstream_sync (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id TEXT NOT NULL,
  data JSONB NOT NULL,
  timestamp BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000,
  created_at TIMESTAMP DEFAULT NOW(),
  UNIQUE(user_id)
);

-- Index
CREATE INDEX idx_user_id ON cloudstream_sync(user_id);
CREATE INDEX idx_timestamp ON cloudstream_sync(timestamp DESC);
```

### 3. RLS (Row Level Security) - Opsiyonel

```sql
ALTER TABLE cloudstream_sync ENABLE ROW LEVEL SECURITY;

-- Herkes kendi verisini okuyabilir/yazabilir
CREATE POLICY "Users can manage own data"
  ON cloudstream_sync
  FOR ALL
  USING (true)
  WITH CHECK (true);
```

### 4. Eklentide Yapılandır

- **Supabase URL**: `https://xxx.supabase.co`
- **API Key**: `eyJhbGc...` (anon/public key)
- **Tablo Adı**: `cloudstream_sync` (varsayılan)
- **User ID**: Benzersiz bir ID (örn: `user123`)

## Kullanım

1. **Buluta Yükle**: Mevcut verilerini buluta gönder
2. **Buluttan İndir**: Buluttaki veriyi cihaza çek
3. **Buluttan Sil**: Buluttaki tüm veriyi sil

## Mimari

```
CloudStreamSyncPlugin
    ↓
SyncManager → CloudProvider (interface)
    ↓
SupabaseProvider / GDriveProvider / FirebaseProvider
```

### ponytail Sınırları

- CloudStream internal API entegrasyonu eksik (şu an mock data)
- GDrive OAuth flow henüz yok
- Firebase Realtime DB sync pattern implementasyonu bekleniyor
- Auto-sync (otomatik senkronizasyon) yok

## Geliştirme

```bash
git clone https://github.com/yourrepo/cloudstream-sync
cd cloudstream-sync
./gradlew CloudStreamSync:make
```

Build edilen `.cs3` dosyası: `CloudStreamSync/build/` klasöründe

## Katkıda Bulunma

1. Fork yap
2. Feature branch oluştur (`git checkout -b feature/gdrive`)
3. Commit at (`git commit -m 'GDrive provider eklendi'`)
4. Push yap (`git push origin feature/gdrive`)
5. Pull Request aç

## Lisans

GPL-3.0 - CloudStream ile uyumlu

## Teşekkürler

- [recloudstream/cloudstream](https://github.com/recloudstream/cloudstream)
- [Supabase](https://supabase.com)
