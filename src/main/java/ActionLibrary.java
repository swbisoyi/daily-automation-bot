import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;

public class ActionLibrary {
    public WebDriver driver;

    // 🔒 SECURE: Reads from GitHub Secrets
    private final String SLACK_WEBHOOK = System.getenv("SLACK_WEBHOOK_URL");

    public void openBrowser() {
        ChromeOptions options = new ChromeOptions();
        // ☁️ SERVER MODE: Headless is mandatory for GitHub Actions
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--remote-allow-origins=*");

        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }

    public void closeBrowser() { if (driver != null) driver.quit(); }
    public void navigate(String url) { driver.get(url); }
    public void tap(String xpath) { driver.findElement(By.xpath(xpath)).click(); }
    public void type(String text, String xpath) { driver.findElement(By.xpath(xpath)).sendKeys(text); }

    public void verifyVisible(String xpath) throws Exception {
        if (!driver.findElement(By.xpath(xpath)).isDisplayed()) {
            throw new Exception("Element not visible: " + xpath);
        }
    }

    public void waitFor(int seconds) {
        try { Thread.sleep(seconds * 1000L); } catch (Exception e) {}
    }

    public void sendSlackNotification(String message) {
        if (SLACK_WEBHOOK == null || SLACK_WEBHOOK.isEmpty()) {
            System.out.println("⚠️ No Slack Webhook found in environment variables. Skipping notification.");
            return;
        }
        try {
            URL url = new URL(SLACK_WEBHOOK);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            // Simple JSON payload
            String jsonPayload = "{\"text\": \"" + message + "\"}";

            try(OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes("UTF-8"));
                os.flush();
            }
            int responseCode = conn.getResponseCode();
            // System.out.println("Slack Response Code: " + responseCode); // Uncomment for debugging
        } catch (Exception e) {
            System.err.println("Failed to send Slack notification.");
            e.printStackTrace();
        }
    }
}