# CashFlowCoin Web App

Profesyonel ve responsive React + TypeScript tabanlı CashFlowCoin SPA arayüzü.

## Geliştirme

Backend `http://localhost:8081` adresinde çalışırken:

```powershell
npm run dev
```

Arayüz:

```text
http://localhost:5173
```

Vite geliştirme sunucusu `/api` isteklerini otomatik olarak backend'e yönlendirir.

## Özellikler

- Kayıt ve giriş ekranı
- Redis session token entegrasyonu
- 15 saniyelik canlı fiyat polling
- BTC, ETH ve SOL piyasa görünümü
- Fiyat geçmişi grafiği
- Alış / satış modalı
- Portföy ve işlem geçmişi
- Gemini destekli CashFlowCoin AI sohbeti
- Loading, skeleton ve kullanıcı dostu hata mesajları
- Masaüstü, tablet ve mobil responsive tasarım

## Production build

```powershell
npm run build
```