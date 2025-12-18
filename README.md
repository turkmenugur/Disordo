# Disordo - Mobil Uygulama

_Disleksi risk analizi için geliştirilmiş yapay zeka destekli Android uygulaması_

Bu depo, **Samsung Innovation Campus** kapsamında geliştirilen **Disordo** mobil uygulamasının kaynak kodlarını içerir.

Uygulama, çocuklarda disleksi riskini **erken aşamada tespit etmek** amacıyla el yazısı görüntülerini analiz eden bir yapay zeka tabanlı mobil çözümdür.

---

## 1. Uygulama Hakkında

**Disordo**, ebeveynlerin ve eğitimcilerin çocuklarda disleksi belirtilerini erken aşamada fark etmelerine yardımcı olan kullanıcı dostu bir Android uygulamasıdır.

### Temel Özellikler

| Özellik | Açıklama |
|---------|----------|
| 📷 **Kamera ile Çekim** | El yazısı örneklerini doğrudan kamera ile çekin |
| 🖼️ **Galeri Desteği** | Galeriden mevcut el yazısı fotoğraflarını yükleyin |
| 🤖 **Yapay Zeka Analizi** | YOLOv8 tabanlı model ile harf tespiti ve hata analizi |
| 📊 **Risk Değerlendirmesi** | Hibrit analiz pipeline ile kapsamlı risk skoru |
| 💡 **Öneriler** | Risk seviyesine göre kişiselleştirilmiş öneriler |
| 🎨 **Modern Tasarım** | Material Design 3 ile kullanıcı dostu arayüz |

---

## 2. Hibrit Analiz Sistemi

Uygulama, disleksi riskini tespit etmek için **iki aşamalı hibrit bir sistem** kullanır:

### Aşama 1: Yapay Zeka (Object Detection)

```
El Yazısı Görüntüsü → YOLOv8 Modeli → Hata Tespiti
```

- **YOLOv8 Nesne Tespiti Modeli** (TensorFlow Lite formatında)
- `Reversal` (Ters Çevirme) ve `Corrected` (Düzeltilmiş) bölgeleri tespit eder
- 'b' ↔ 'd', 'p' ↔ 'q' gibi harf karışıklıklarını algılar

### Aşama 2: Algoritmik Analiz (Line Error)

```
Tespit Koordinatları → Line Error Algoritması → Sapma Skoru
```

- Tespit edilen harflerin koordinatlarını analiz eder
- Satır takip hatasını (dikey sapma) matematiksel olarak hesaplar
- Harflerin satırdan sapma oranını ölçer

### Risk Hesaplaması

```
Final Risk = (Reversal Sayısı × Ağırlık) + (Line Error Skoru × Ağırlık)
```

Bu hibrit yaklaşım, bir uzmanın analiz sürecini taklit ederek daha güvenilir sonuçlar sağlar.

---





## 3. Proje

Bu proje, **Samsung Innovation Campus** programı kapsamında geliştirilmiştir.


---

## 8. Model Eğitim Reposu

Bu mobil uygulama tarafından kullanılan makine öğrenimi modellerinin eğitim ve geliştirme süreçleri **[Disordo Model Eğitim Deposu](https://github.com/ozeraysenur/Disordo-model-training)** adresinde ayrıntılı olarak açıklanmaktadır.

---



Bu proje eğitim amaçlı geliştirilmiştir.

---

<div align="center">

**Erken Tespit, Güçlü Gelecek**

_Disleksi farkındalığı için bir adım_

</div>
