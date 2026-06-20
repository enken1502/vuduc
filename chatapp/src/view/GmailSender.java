package view;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Base64;
import java.util.Random;
import javax.net.ssl.SSLSocketFactory;

public class GmailSender {
    // 🌟 ĐIỀN EMAIL CỦA BẠN VÀ MẬT KHẨU ỨNG DỤNG 16 KÝ TỰ VÀO ĐÂY
    private static final String EMAIL_SENDER = "vduc2503@gmail.com";
    private static final String EMAIL_APP_PASSWORD = "iyyxxlrvemnuzkoz"; // Viết liền 16 ký tự không dấu cách

    public static String generateOTP() {
        Random random = new Random();
        return String.valueOf(100000 + random.nextInt(900000));
    }

    public static boolean sendOTP(String recipientEmail, String otpCode) {
        String smtpHost = "smtp.gmail.com";
        int smtpPort = 465; // Cổng SSL bảo mật của Google SMTP

        // Sử dụng SSLSocket để kết nối bảo mật trực tiếp với Google
        try (Socket socket = SSLSocketFactory.getDefault().createSocket(smtpHost, smtpPort);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             OutputStream out = socket.getOutputStream()) {

            // Đọc dòng chào mừng đầu tiên từ Server Google (Mã 220)
            readResponse(in);

            // 1. Gửi lệnh EHLO để chào hỏi Server
            sendCmd(out, "EHLO " + smtpHost);
            readResponse(in);

            // 2. Gửi yêu cầu Đăng nhập bằng AUTH LOGIN
            sendCmd(out, "AUTH LOGIN");
            readResponse(in);

            // 3. Gửi Username (Email của bạn) đã được mã hóa Base64
            String base64User = Base64.getEncoder().encodeToString(EMAIL_SENDER.getBytes());
            sendCmd(out, base64User);
            readResponse(in);

            // 4. Gửi Mật khẩu ứng dụng đã được mã hóa Base64
            String base64Pass = Base64.getEncoder().encodeToString(EMAIL_APP_PASSWORD.getBytes());
            sendCmd(out, base64Pass);
            readResponse(in); // Nếu đăng nhập thành công sẽ trả về mã 235

            // 5. Khai báo Email người gửi
            sendCmd(out, "MAIL FROM:<" + EMAIL_SENDER + ">");
            readResponse(in);

            // 6. Khai báo Email người nhận (Email đăng ký)
            sendCmd(out, "RCPT TO:<" + recipientEmail + ">");
            readResponse(in);

            // 7. Gửi lệnh DATA để chuẩn bị viết nội dung Email
            sendCmd(out, "DATA");
            readResponse(in); // Nhận mã 354 để bắt đầu truyền nội dung

            // 8. Định dạng nội dung Mail theo chuẩn giao thức Internet (Bắt buộc đúng cấu trúc này)
            String emailContent = "From: " + EMAIL_SENDER + "\r\n" +
                                  "To: " + recipientEmail + "\r\n" +
                                  "Subject: Ma xac thuc OTP dang ky tai khoan\r\n" +
                                  "Content-Type: text/html; charset=utf-8\r\n" +
                                  "\r\n" + // Dòng trống ngăn cách giữa Header và Body
                                  "<h3>MA XAC THUC DANG KY</h3>" +
                                  "<p>Ma OTP cua ban la: <b style='font-size:24px;color:red;'>" + otpCode + "</b></p>" +
                                  "\r\n.\r\n"; // Dấu chấm đứng riêng một dòng để báo kết thúc nội dung dữ liệu

            sendCmd(out, emailContent);
            readResponse(in); // Nhận mã 250 báo gửi thành công

            // 9. Gửi lệnh QUIT để ngắt kết nối an toàn với máy chủ Google
            sendCmd(out, "QUIT");
            
            System.out.println("📨 [GMAIL SOCKET]: Đã dùng Email cá nhân gửi thành công OTP [" + otpCode + "] tới " + recipientEmail);
            return true;

        } catch (Exception e) {
            System.err.println("❌ [GMAIL SOCKET ERROR]: Gửi mail thất bại: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static void sendCmd(OutputStream out, String cmd) throws Exception {
        out.write((cmd + "\r\n").getBytes("UTF-8"));
        out.flush();
    }

    private static void readResponse(BufferedReader in) throws Exception {
        String line = in.readLine();
        // Server SMTP của Google có thể trả về nhiều dòng chào mừng, đọc đến khi dòng không có dấu gạch ngang
        while (line != null && line.length() >= 4 && line.charAt(3) == '-') {
            line = in.readLine();
        }
    }
}