# CloudStream Sync - Manuel Repo Paylaşımı (Workflow'suz)

## 📋 Gerekli Dosyalar

CloudStream eklenti reposunda 2 dosya gerekli:

### 1. `repo.json` (Repo metadata)
```json
{
    "name": "CloudStream Sync Repository",
    "description": "Multi-cloud sync eklentisi",
    "manifestVersion": 1,
    "pluginLists": [
        "https://raw.githubusercontent.com/USER/REPO/master/plugins.json"
    ]
}
```

### 2. `plugins.json` (Eklenti listesi)
```json
[
    {
        "url": "https://raw.githubusercontent.com/USER/REPO/master/CloudStreamSync.cs3",
        "status": 1,
        "version": 1,
        "name": "CloudStreamSync",
        "internalName": "CloudStreamSync",
        "authors": ["CloudStreamSync Team"],
        "description": "Multi-cloud sync for bookmarks, watch history, search history, extensions & settings",
        "fileSize": 19403,
        "repositoryUrl": "https://github.com/USER/REPO",
        "language": "tr",
        "tvTypes": [],
        "iconUrl": "https://raw.githubusercontent.com/recloudstream/cloudstream/master/app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png",
        "apiVersion": 1
    }
]
```

---

## 🚀 Manuel Yayınlama (Workflow'suz)

### Adım 1: Yerel Build

```bash
cd D:\Projeler\CloudStream\Sync
.\gradlew.bat CloudStreamSync:make
```

Build edilen dosya: `CloudStreamSync/build/CloudStreamSync.cs3`

### Adım 2: GitHub Repo Oluştur

1. GitHub'da yeni repo: `cloudstream-sync`
2. Public olarak oluştur

### Adım 3: Dosyaları Hazırla

```bash
# Repo root'a cs3 dosyasını kopyala
Copy-Item CloudStreamSync/build/CloudStreamSync.cs3 .

# repo.json oluştur
# plugins.json oluştur (aşağıdaki template'i kullan)
```

### Adım 4: Git Push

```bash
git add repo.json plugins.json CloudStreamSync.cs3
git commit -m "Release v1"
git push origin master
```

### Adım 5: CloudStream'e Ekle

Kullanıcılar şu linki tarayıcıda açacak:

```
https://raw.githubusercontent.com/USER/cloudstream-sync/master/repo.json
```

CloudStream otomatik olarak açacak ve depoyu yükleyecek.

---

## 📦 Güncelleme (Her Yeni Versiyonda)

### 1. Version Artır

`CloudStreamSync/build.gradle.kts`:
```kotlin
version = 2  // artır
```

### 2. Yeniden Build

```bash
.\gradlew.bat CloudStreamSync:make
```

### 3. plugins.json Güncelle

- `version` değerini artır
- `fileSize` güncellesin (yeni .cs3 boyutu)

### 4. Push

```bash
Copy-Item CloudStreamSync/build/CloudStreamSync.cs3 . -Force
git add .
git commit -m "v2: GDrive desteği eklendi"
git push
```

CloudStream otomatik güncellemeleri görecek.

---

## 🎯 Avantajlar / Dezavantajlar

### ✅ Manuel Yöntem (Workflow'suz)

**Artıları:**
- Basit, GitHub Actions yok
- Tam kontrol
- Hızlı test

**Eksileri:**
- Her güncellemede manuel build + push
- `plugins.json` elle güncellemen gerek
- Hata riski

### ✅ Otomatik Yöntem (Workflow'lu)

**Artıları:**
- Push → otomatik build + deploy
- `plugins.json` otomatik oluşur
- Hatasız

**Eksileri:**
- Workflow setup karmaşık
- GitHub Actions bilgisi gerek

---

## 💡 Öneri: Hybrid Yaklaşım

İlk aşamada **manuel** ilerle, stable olunca **workflow** ekle:

1. ✅ Şimdi: Manuel build + push (hızlı test)
2. 🔜 Sonra: Basit workflow (sadece build)
3. 🔜 Gelecek: Tam workflow (build + publish + test)

---

## 📝 Tam Örnek: plugins.json

```json
[
    {
        "url": "https://raw.githubusercontent.com/burak-ayd/cloudstream-sync/master/CloudStreamSync.cs3",
        "status": 3,
        "version": 1,
        "name": "CloudStreamSync",
        "internalName": "CloudStreamSync",
        "authors": ["burak-ayd"],
        "description": "Multi-cloud sync for bookmarks, watch history, search history, extensions & settings. Supports Supabase (GDrive & Firebase coming soon).",
        "fileSize": 19403,
        "repositoryUrl": "https://github.com/burak-ayd/cloudstream-sync",
        "language": "tr",
        "tvTypes": [],
        "iconUrl": "https://raw.githubusercontent.com/recloudstream/cloudstream/master/app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png",
        "apiVersion": 1
    }
]
```

**Status kodları:**
- `0`: Down
- `1`: Ok
- `2`: Slow
- `3`: Beta

---

## 🔗 Kullanım

Kullanıcılar CloudStream'de:

1. Ayarlar → Eklentiler → Depo ekle
2. URL: `https://raw.githubusercontent.com/burak-ayd/cloudstream-sync/master/repo.json`
3. Depo yüklendi → CloudStreamSync görünecek
4. İndir → Yeniden başlat

Artık eklenti kullanılabilir!
