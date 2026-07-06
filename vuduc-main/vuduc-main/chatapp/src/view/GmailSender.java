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
    // Sửa từ "iyxxlrvemnuzkoz" thành chuỗi đầy đủ 16 ký tự dưới đây:
private static final String EMAIL_APP_PASSWORD = "iyyx xrlv emnu zkoz";//

    public static String generateOTP() {
        Random random = new Random();
        return String.valueOf(100000 + random.nextInt(900000));
    }

    public static boolean sendOTP(String recipientEmail, String otpCode) {
        String smtpHost = "smtp.gmail.com";
        int smtpPort = 465; 

        try (Socket socket = SSLSocketFactory.getDefault().createSocket(smtpHost, smtpPort);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
             OutputStream out = socket.getOutputStream()) {

            readResponse(in);

            sendCmd(out, "EHLO " + smtpHost);
            readResponse(in);

            sendCmd(out, "AUTH LOGIN");
            readResponse(in);

            String base64User = Base64.getEncoder().encodeToString(EMAIL_SENDER.getBytes());
            sendCmd(out, base64User);
            readResponse(in);

            String base64Pass = Base64.getEncoder().encodeToString(EMAIL_APP_PASSWORD.getBytes());
            sendCmd(out, base64Pass);
            readResponse(in); 

            sendCmd(out, "MAIL FROM:<" + EMAIL_SENDER + ">");
            readResponse(in);

            sendCmd(out, "RCPT TO:<" + recipientEmail + ">");
            readResponse(in);

            sendCmd(out, "DATA");
            readResponse(in); 

            // Cấu hình định dạng ID tin nhắn ngẫu nhiên theo thời gian thực để Google không chặn thô
            String messageId = "<" + System.currentTimeMillis() + "@gmail.com>";

            // Chuỗi Content đầy đủ các thông số định danh chuẩn MIME mã hóa
            String emailContent = "From: CHAT APP SYSTEM <" + EMAIL_SENDER + ">\r\n" +
                "To: <" + recipientEmail + ">\r\n" +
                "Subject: Ma xac thuc OTP dang ky tai khoan\r\n" +
                "MIME-Version: 1.0\r\n" +
                "Message-ID: " + messageId + "\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Content-Transfer-Encoding: 8bit\r\n" +
                "\r\n" + 
                "<html><body>" +
                "<h3>MA XAC THUC DANG KY TAI KHOAN</h3>" +
                "<p>Ma OTP cua ban la: <b style='font-size:24px;color:red;'>" + otpCode + "</b></p>" +
                "<p style='color:#888; font-size:12px;'>Vui long khong chia se ma nay cho bat ky ai.</p>" +
                "</body></html>" +
                "\r\n.\r\n"; 

            sendCmd(out, emailContent);
            readResponse(in); 

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
    // In trực tiếp phản hồi của Google ra Terminal để xem nó báo lỗi gì
    System.out.println("   -> [Google SMTP]: " + line); 
    
    // Nếu gặp mã lỗi đầu 4xx hoặc 5xx (Ví dụ: 535 Authentication Failed)
    if (line != null && (line.startsWith("4") || line.startsWith("5"))) {
        throw new Exception("Google SMTP tu choi: " + line);
    }

    while (line != null && line.length() >= 4 && line.charAt(3) == '-') {
        line = in.readLine();
        System.out.println("   -> [Google SMTP]: " + line);
        if (line.startsWith("4") || line.startsWith("5")) {
            throw new Exception("Google SMTP tu choi: " + line);
        }
    }
}
}