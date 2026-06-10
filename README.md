# vsl-backend
Mã nguồn Backend (Java Spring Boot) cho Ứng dụng Học Ngôn ngữ Ký hiệu Việt Nam (VSL)

**Stack:** Java 21 · Spring Boot 4 · Spring Security + JWT · PostgreSQL · Docker

---

## 🚀 Chạy bằng Docker (khuyến nghị)

Toàn bộ hệ thống (app + database PostgreSQL) chạy bằng Docker, không cần cài Java/Maven trên máy.

### 1. Yêu cầu
- **Docker Desktop** đã cài và **đang chạy**.
- (Tùy chọn) JDK 21 nếu muốn mở code / chạy test trong IDE.

### 2. Tạo file `.env` (BẮT BUỘC)
File `.env` chứa thông tin nhạy cảm nên **không** được commit lên git. Mỗi người tự tạo từ template:

```powershell
# Windows PowerShell
Copy-Item .env.example .env
```
```bash
# macOS / Linux
cp .env.example .env
```

Sau đó mở `.env` và điền giá trị:
| Biến | Ghi chú |
|------|---------|
| `POSTGRES_PASSWORD` | Mật khẩu DB tùy chọn |
| `DB_USERNAME` / `DB_PASSWORD` | Phải **khớp** với `POSTGRES_USER` / `POSTGRES_PASSWORD` |
| `JWT_SECRET` | Chuỗi Base64 ≥ 256-bit. Tạo mới: `openssl rand -base64 64` |

> ⚠️ Thiếu `.env` thì container sẽ không khởi động được.

### 3. Build & chạy
```bash
docker compose up -d --build
```

### 4. Kiểm tra
```bash
docker compose ps
curl http://localhost:8080/api/health
```
Kết quả mong đợi: cả 2 container `vsl-backend` và `vsl-postgres` ở trạng thái `healthy`.

---

## 🐳 Thành phần Docker

| Loại | Tên | Mô tả |
|------|-----|-------|
| Container | `vsl-backend` | App Spring Boot — cổng `8080` |
| Container | `vsl-postgres` | PostgreSQL 17 — cổng `5432` |
| Image | `vsl-backend-app` | Build cục bộ từ `Dockerfile` |
| Volume | `vsl-backend_vsl-pgdata` | Lưu dữ liệu DB (giữ lại khi tắt container) |
| Network | `vsl-backend_vsl-net` | Mạng nội bộ giữa app ↔ db |

---

## 🛠️ Các lệnh thường dùng

```bash
docker compose up -d --build     # Build lại & chạy (dùng sau khi sửa code)
docker compose logs -f app       # Xem log app theo thời gian thực
docker compose ps                # Trạng thái container
docker compose down              # Dừng (GIỮ dữ liệu DB)
docker compose down -v           # Dừng + XÓA dữ liệu DB (volume)
docker compose restart app       # Khởi động lại riêng app
```

---

## ⚠️ Xử lý lỗi thường gặp

| Triệu chứng | Nguyên nhân & cách xử lý |
|-------------|--------------------------|
| Container không khởi động | Thiếu file `.env` → tạo từ `.env.example` |
| `port is already allocated` | Cổng `5432`/`8080` đang bị chiếm → đổi `POSTGRES_PORT` / `SERVER_PORT` trong `.env` |
| App báo lỗi kết nối DB | Postgres chưa sẵn sàng → app đã cấu hình chờ DB `healthy`, thử `docker compose up -d` lại |
| Dữ liệu DB không đồng bộ giữa các máy | Bình thường — volume là cục bộ từng máy; schema tự tạo nhờ `ddl-auto=update` |

---

## 📡 API Endpoints

Base URL: `http://localhost:8080`

| Method | Endpoint | Quyền | Mô tả |
|--------|----------|-------|-------|
| `GET`  | `/api/health` | Public | Kiểm tra service sống |
| `POST` | `/api/auth/register` | Public | Đăng ký tài khoản (role `USER`) |
| `POST` | `/api/auth/login` | Public | Đăng nhập → access + refresh token |
| `POST` | `/api/auth/refresh` | Public | Cấp access token mới (token rotation) |
| `POST` | `/api/auth/logout` | Auth | Thu hồi refresh token |
| `GET`  | `/api/auth/me` | Auth | Thông tin tài khoản hiện tại |

### Ví dụ đăng ký
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"johndoe","email":"john@example.com","fullName":"John Doe","password":"Password1"}'
```

### Định dạng response chuẩn
**Thành công:**
```json
{ "success": true, "code": "SUCCESS", "message": "...", "data": { }, "timestamp": "..." }
```
**Lỗi:**
```json
{ "status": 401, "code": "AUTH_1003", "error": "INVALID_CREDENTIALS", "message": "...", "path": "...", "timestamp": "..." }
```

---

## 💻 Chạy không cần Docker (dev trong IDE)

Cần JDK 21 + một PostgreSQL đang chạy (có thể chỉ bật mỗi DB: `docker compose up -d postgres`).
`.env` để `DB_URL=jdbc:postgresql://localhost:5432/vsl_db`, rồi:
```bash
./mvnw spring-boot:run     # chạy app
./mvnw test                # chạy test (dùng H2 in-memory, không cần DB)
```
