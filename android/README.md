# App Android "Đấu thầu XN"

- Giao diện: `app.html` (bảng tin, theo dõi bệnh viện, cài đặt) và `index.html` (theo dõi thầu XN), tải từ web nên sửa web là app đổi theo.
- Phần native (Java, không thư viện ngoài): lưu danh sách follow, gọi API công khai muasamcong từ điện thoại (IP Việt Nam), chạy nền ~30 phút/lần bằng JobScheduler, bắn thông báo.
- Build: `DT_KEYSTORE=... DT_KS_PASS=... VC=2 VN=1.1 ./build.sh` (cần Android SDK build-tools 35 + platform 35, JDK 17+). File ra: `build/dauthau.apk`.
- Keystore KHÔNG nằm trong repo. Mọi bản cập nhật phải ký cùng keystore thì điện thoại mới cho cài đè.
- Phát hành: chép APK vào `app/dauthau.apk`, tăng `app/version.json`.
