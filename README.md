# 🍽️ GastroLink

> Ứng dụng đặt món ăn thông minh tích hợp AI dinh dưỡng — **Android · Kotlin · Jetpack Compose**

GastroLink là ứng dụng đặt món ăn nhà hàng thế hệ mới, kết hợp hệ thống **theo dõi dinh dưỡng thời gian thực**, **trợ lý AI cá nhân** và **đồng bộ đám mây MySQL** để mang đến trải nghiệm ăn uống lành mạnh, thông minh và tiện lợi.

---

## 📸 Màn Hình Ứng Dụng

| Thực đơn | Giỏ hàng | Thống kê | Trợ lý AI |
|:---:|:---:|:---:|:---:|
| Xem menu với đầy đủ thông tin dinh dưỡng | Quản lý giỏ hàng theo dõi Calo realtime | Biểu đồ dinh dưỡng chi tiết | Chat AI tư vấn món ăn |

---

## ✨ Tính Năng Chính

### 🤖 AI Dinh Dưỡng (Groq Llama)
- **Chat trợ lý dinh dưỡng**: Hỏi đáp thông minh với AI về chế độ ăn, gợi ý món phù hợp mục tiêu
- **Đề xuất món ăn cá nhân hóa**: AI phân tích hồ sơ sức khỏe và đưa ra lựa chọn tối ưu
- **Quét ảnh món ăn (Vision AI)**: Chụp ảnh thực phẩm để nhận diện tên, Calo, Protein, Carbs, Fat tức thì

### 🍽️ Quản Lý Thực Đơn
- Thực đơn phong phú từ **4 chi nhánh** (Hà Nội, Hải Phòng, Đà Nẵng, TP.HCM)
- Tích hợp **TheMealDB API** — kho 300+ công thức món ăn quốc tế dịch sang Tiếng Việt
- Thông tin dinh dưỡng đầy đủ: Calo (kcal), Protein, Carbs, Chất béo
- Lọc món theo **chế độ ăn** (giảm cân, tăng cơ, duy trì) và **dị ứng thực phẩm**

### 🛒 Đặt Hàng Thông Minh
- Đặt món **Solo** hoặc **Nhóm** với theo dõi tổng Calo theo thời gian thực
- Giỏ hàng thông minh hiển thị tổng dinh dưỡng của bữa ăn
- Lịch sử đơn hàng đầy đủ, xem lại chi tiết từng bữa

### 📊 Theo Dõi Sức Khỏe
- **Hồ sơ cá nhân**: Tuổi, cân nặng, chiều cao, mục tiêu (Giảm cân / Tăng cơ / Duy trì)
- **Biểu đồ dinh dưỡng**: Thống kê Calo hàng ngày, phân bổ Protein/Carbs/Fat
- **Kế hoạch thực đơn tuần**: Gợi ý thực đơn cân đối dựa trên mục tiêu sức khỏe
- **Chế độ Dinh dưỡng**: Tính toán TDEE, BMR và mức tiêu thụ calo mục tiêu

### ☁️ Đồng Bộ Đám Mây (MySQL Cloud Sync)
- **Sao lưu dữ liệu** lên MySQL: Hồ sơ, dị ứng, lịch sử đặt món
- **Khôi phục tức thì** khi đổi thiết bị hoặc cài lại ứng dụng
- Kiến trúc Graceful Fallback — ứng dụng không bao giờ crash dù mất kết nối

### 🔒 Quyền Riêng Tư
- Xuất dữ liệu cá nhân (JSON/CSV)
- Xóa toàn bộ dữ liệu cục bộ với một thao tác

---

## 🏛️ Kiến Trúc Hệ Thống

```
┌─────────────────────────┐     REST API (JSON)     ┌──────────────────────────┐
│  Android Client         │ ◄──────────────────────► │  Node.js Express Proxy   │
│  Kotlin + Jetpack Compose│                         │  (AI Proxy + MySQL Sync) │
└─────────────────────────┘                         └────────────┬─────────────┘
                                                                 │
                                              ┌──────────────────┼──────────────────┐
                                              │                  │                  │
                                    ┌─────────▼──────┐  ┌───────▼───────┐  ┌──────▼──────┐
                                    │  MySQL Database │  │  Groq Llama   │  │ TheMealDB   │
                                    │  (Cloud Sync)   │  │  (AI/Vision)  │  │  (Menu API) │
                                    └────────────────┘  └───────────────┘  └─────────────┘
```

### Kiến trúc Android (MVVM + Clean Architecture)

```
app/src/main/java/tech/davidmartinezmuelas/gastrolink/
├── MainActivity.kt
├── ui/
│   ├── AppViewModel.kt          # ViewModel trung tâm (State management)
│   ├── screens/                 # 14 màn hình Jetpack Compose
│   │   ├── MenuScreen.kt        # Thực đơn & filter dinh dưỡng
│   │   ├── CartScreen.kt        # Giỏ hàng realtime
│   │   ├── ChatScreen.kt        # Chat AI dinh dưỡng
│   │   ├── ProfileScreen.kt     # Hồ sơ sức khỏe
│   │   ├── StatsScreen.kt       # Biểu đồ & thống kê
│   │   ├── HistoryScreen.kt     # Lịch sử đặt hàng
│   │   ├── SettingsScreen.kt    # Cài đặt & Cloud Sync
│   │   └── ...
│   └── components/              # UI components tái sử dụng
├── data/                        # Repositories & Data Sources
├── domain/                      # Business Logic & Use Cases
└── model/                       # Data Models
```

---

## 🛠️ Công Nghệ Sử Dụng

### Android Client
| Công nghệ | Phiên bản | Mục đích |
|---|---|---|
| **Kotlin** | 1.9+ | Ngôn ngữ chính |
| **Jetpack Compose** | BOM 2024.06 | UI Framework hiện đại |
| **Room Database** | 2.6.1 | Cơ sở dữ liệu cục bộ (SQLite) |
| **Ktor Client** | 2.3.12 | HTTP Client (gọi API) |
| **DataStore Preferences** | 1.1.1 | Lưu trữ cài đặt |
| **Navigation Compose** | 2.8.0 | Điều hướng màn hình |
| **Coil** | 2.6.0 | Tải & cache hình ảnh |
| **Material 3** | — | Hệ thống thiết kế |

### Backend (Node.js Proxy)
| Công nghệ | Phiên bản | Mục đích |
|---|---|---|
| **Node.js** | ≥ 18 | Runtime |
| **Express** | 4.21 | Web Framework |
| **mysql2** | 3.22 | Kết nối MySQL (Connection Pool) |
| **openai SDK** | 4.104 | Tích hợp Groq Llama AI |
| **express-rate-limit** | 7.5 | Bảo vệ Rate Limit |

### AI & External APIs
- **Groq Cloud** — Inference tốc độ cao cho model Llama 3.3 70B & Llama 4 Scout Vision
- **TheMealDB API** — Kho dữ liệu công thức món ăn quốc tế (miễn phí)

---

## 🚀 Hướng Dẫn Cài Đặt & Chạy

### Yêu cầu hệ thống
- **Android Studio** Hedgehog (2023.1.1) trở lên
- **Node.js** v18+
- **MySQL Server** (XAMPP, Laragon, Docker, hoặc cloud)
- Tài khoản **Groq Cloud** (miễn phí) để lấy API Key

---

### 1️⃣ Clone dự án

```bash
git clone <repository-url>
cd GastroLink
```

---

### 2️⃣ Cài đặt & Chạy Backend (Node.js)

```bash
cd server
```

Sao chép file cấu hình môi trường:
```bash
cp .env.example .env
```

Chỉnh sửa file `.env` với thông tin của bạn:
```env
PORT=3000
GROQ_API_KEY=your_groq_api_key_here
AI_PROXY_TOKEN=your_secret_token_here

# Kết nối MySQL (bỏ qua nếu không dùng Cloud Sync)
DB_HOST=localhost
DB_USER=root
DB_PASSWORD=
DB_NAME=gastrolink
```

Cài đặt dependencies và khởi động server:
```bash
npm install
npm start
```

> ✅ Khi thành công sẽ thấy: `🚀 GastroLink AI Proxy đang chạy tại cổng 3000`

---

### 3️⃣ Thiết lập Database MySQL (Tùy chọn — cho Cloud Sync)

1. Mở **XAMPP** → Start **MySQL**
2. Truy cập `http://localhost/phpmyadmin/`
3. Tạo database tên `gastrolink`
4. Import file schema:

```bash
mysql -u root gastrolink < server/schema.sql
```

---

### 4️⃣ Cài đặt & Chạy Android App

1. Mở thư mục gốc dự án bằng **Android Studio**
2. Đợi Gradle sync hoàn tất
3. Chạy app trên **Emulator** (API 26+) hoặc thiết bị thật

> 💡 **Debug mode** đã được cấu hình sẵn để kết nối tới `http://10.0.2.2:3000` (địa chỉ localhost của máy tính từ Emulator Android).

---

## 🔑 Cấu Hình AI (Build Config)

Trong `app/build.gradle.kts`, các giá trị AI có thể được cấu hình qua biến môi trường khi build:

```bash
# Ví dụ build với AI token tùy chỉnh
AI_PROXY_TOKEN=your_token ./gradlew assembleDebug
```

| Build Type | AI_ENABLED | AI_BASE_URL |
|---|---|---|
| `debug` | `true` | `http://10.0.2.2:3000` |
| `release` | `false` | _(cần cấu hình thủ công)_ |

---

## 📡 API Backend Endpoints

| Method | Endpoint | Mô tả | Auth |
|---|---|---|---|
| `GET` | `/health` | Kiểm tra trạng thái server | ❌ |
| `POST` | `/ai/recommendation` | Đề xuất món ăn AI | ✅ |
| `POST` | `/ai/chat` | Chat trợ lý dinh dưỡng | ✅ |
| `POST` | `/ai/scan-dish` | Nhận diện món ăn từ ảnh (Vision) | ✅ |
| `GET` | `/catalog/branches` | Danh sách chi nhánh | ✅ |
| `GET` | `/catalog/external-dishes` | Thực đơn từ TheMealDB (có dịch AI) | ✅ |
| `GET` | `/proxy-image` | Proxy hình ảnh tránh CORS | ❌ |
| `POST` | `/ai/sync/upload` | Đồng bộ dữ liệu lên MySQL | ✅ |
| `GET` | `/ai/sync/download` | Tải dữ liệu từ MySQL về app | ✅ |

> **Auth**: Yêu cầu header `X-API-KEY: <AI_PROXY_TOKEN>`

---

## 🗃️ Cấu Trúc Database MySQL

```sql
-- Hồ sơ dinh dưỡng người dùng
user_profiles (userId, age, weight, height, goal, allergies, updatedAt)

-- Lịch sử đơn hàng & Calo tiêu thụ  
user_orders (orderId, userId, orderDate, totalKcal, totalProtein, totalCarbs, totalFat, itemsJson, updatedAt)
```

---

## 📂 Cấu Trúc Dự Án

```
GastroLink/
├── app/                        # Android App (Kotlin + Compose)
│   ├── src/main/
│   │   ├── java/tech/.../gastrolink/
│   │   │   ├── ui/screens/     # 14 màn hình ứng dụng
│   │   │   ├── data/           # Repository & DataSource
│   │   │   ├── domain/         # Business Logic
│   │   │   └── model/          # Data Models
│   │   └── res/                # Resources (layouts, strings, icons)
│   └── build.gradle.kts        # Cấu hình build Android
├── server/                     # Node.js Backend
│   ├── src/
│   │   ├── index.js            # Express server + API routes
│   │   └── db.js               # MySQL connection & queries
│   ├── schema.sql              # Database schema
│   └── .env.example            # Mẫu biến môi trường
├── docs/
│   ├── screenshots/            # Ảnh chụp màn hình
│   └── diagrams/               # Sơ đồ kiến trúc
└── mysql_cloud_sync_walkthrough.md  # Hướng dẫn Cloud Sync chi tiết
```

---

## 🌐 Chi Nhánh GastroLink

| ID | Tên Chi Nhánh | Thành Phố |
|---|---|---|
| b001 | GastroLink Trung Tâm | Hà Nội |
| b002 | GastroLink Phía Bắc | Hải Phòng |
| b003 | GastroLink Hải Cảng | Đà Nẵng |
| b004 | GastroLink Phía Nam | TP. Hồ Chí Minh |

---

## 📝 Ghi Chú Phát Triển

- **Graceful Fallback**: Tất cả tính năng AI đều có chế độ dự phòng — ứng dụng hoạt động bình thường kể cả khi không có API Key hoặc mất kết nối mạng.
- **Rate Limiting**: Backend giới hạn 30 requests/phút cho endpoint AI để bảo vệ quota.
- **Min SDK**: Android 8.0 (API 26) trở lên.

---

## 📄 License

Dự án được phát triển cho mục đích học tập tại **Trường Đại học Kinh tế - Kỹ thuật Công nghiệp (VKU)**.

---

<div align="center">

**Made with ❤️ — GastroLink Team · VKU · 2026**

</div>
