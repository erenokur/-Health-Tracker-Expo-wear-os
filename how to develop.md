# Wear OS Geliştirme Ortamı Kurulumu (İlk Kez)

Saatin muhtemelen USB girişi yok, bu yüzden bilgisayara Wi-Fi üzerinden ADB
ile bağlanacağız. Bilgisayar ve saat aynı Wi-Fi ağında olmalı.

## 1. Saatte Geliştirici Seçeneklerini Aç

- Saatte: **Ayarlar → Genel → Saat hakkında → Yazılım**
- **"Yazılım sürümü"** yazısına art arda 5 kez dokun, "Geliştirici modu
  açıldı" mesajını görene kadar

## 2. Hata Ayıklamayı Etkinleştir

- **Ayarlar → Geliştirici seçenekleri** (artık Genel altında görünür)
- **ADB hata ayıklama**'yı aç
- **Kablosuz hata ayıklama (Wireless debugging)** menüsüne girin ve açın.
  _(Not: Eski saatlerde "Wi-Fi üzerinden hata ayıklama" yazar ve IP adresi ile sabit `5555` portunu gösterir. Wear OS 3/4+ cihazlarda port her defasında değişir)._

## 3. Eşleştirme ve Bilgisayardan Bağlanma (Wear OS 3 ve 4+)

Yeni nesil saatlerde (ör. Galaxy Watch 4/5/6) bağlanmadan önce bilgisayarı saatle **eşleştirmeniz** gerekir. (Eğer eski bir cihaz kullanıyorsanız ve port `5555` ise bu adımı atlayıp doğrudan `adb connect IP:5555` yapabilirsiniz).

1. Saatte "Kablosuz hata ayıklama" menüsünün içindeyken aşağı inin ve **"Yeni cihazla eşleştir (Pair with new device)"** seçeneğine dokunun.
2. Ekranda bir **Wi-Fi eşleştirme kodu** ve IP adresi ile Port (örn: `192.168.1.6:33221`) göreceksiniz.
3. Bilgisayarda terminali açıp şu komutu girin (saatteki IP ve portu yazın):
   ```bash
   adb pair 192.168.1.6:33221
   ```
4. İstendiğinde saatteki **Wi-Fi eşleştirme kodunu** girin. Başarılı olduğuna dair mesaj göreceksiniz.

## 4. Bağlantıyı Kur (adb connect)

Eşleştirme ekranından geri çıkıp "Kablosuz hata ayıklama" ana ekranına dönün.
Orada "IP adresi ve Bağlantı noktası" altında **farklı bir port** göreceksiniz (örn: `192.168.1.6:44769`).

Şimdi bu ana ekrandaki port ile bağlanın:

```bash
adb connect 192.168.1.6:44769
```

_(Dikkat: Eşleştirme portu ile bağlantı portu **farklıdır**! Eşleşmeyi yaptıktan sonra ana ekrandaki portu kullanmalısınız)._

## 5. Doğrula

```bash
adb devices
```

Saat `device` olarak listelenmeli (`unauthorized` diyorsa saat ekranındaki
bekleyen onay penceresine bak; `offline` diyorsa `adb disconnect` sonra
tekrar `adb connect` dene).

## Sorun Giderme

- **IP gösterilmiyor / "Wi-Fi üzerinden hata ayıklama" gri**: bazı Samsung
  Galaxy Watch yazılım sürümlerinde saatin doğrudan Wi-Fi'ye bağlı olması
  gerekir (sadece telefon üzerinden Bluetooth ile değil) — önce
  **Ayarlar → Bağlantılar → Wi-Fi**'yi kontrol et.
- **Bağlantı sürekli kopuyor**: saatin Wi-Fi radyosu pil tasarrufu için
  agresif bir şekilde uyur; aktif geliştirme sırasında saat ekranını
  açık/uyanık tut, ya da her oturumda `adb connect`'i tekrar çalıştırmayı
  normal karşıla — bu kalıcı olarak "düzeltilecek" bir şey değil.
- **`adb: failed to connect`**: bilgisayarındaki güvenlik duvarının 5555
  portunu engellemediğinden emin ol, IP'nin değişmediğini kontrol et
  (router DHCP kirası yenilenirse IP değişebilir).

## Saat Bağlandıktan Sonra

```bash
cd wear-app
./gradlew installDebug
```

`./gradlew` komutu yoksa, önce Gradle wrapper'ı oluşturman gerekiyor —
ana `README.md` dosyasındaki "Build & install" bölümüne bak.
