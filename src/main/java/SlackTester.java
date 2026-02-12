import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SlackTester {
    // 🚨 PASTE YOUR WEBHOOK URL HERE AGAIN TO TEST 🚨
    static final String WEBHOOK_URL = "YOUR_WEBHOOK_URL_HERE";

    public static void main(String[] args) {
        System.out.println("🚀 Testing Slack Connection...");
        try {
            URL url = new URL(WEBHOOK_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            String json = "{\"text\": \"🔔 This is a TEST message from Java!\"}";

            OutputStream os = conn.getOutputStream();
            os.write(json.getBytes());
            os.flush();

            int code = conn.getResponseCode();
            System.out.println("📡 Response Code: " + code);
            System.out.println("✅ Response Message: " + conn.getResponseMessage());

            conn.disconnect();
        } catch (Exception e) {
            System.out.println("❌ Connection Failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}