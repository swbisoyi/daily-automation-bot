import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.Collections;

public class ActionLibrary {
    public WebDriver driver;

    // 🔒 SECURE: Reads from Environment Variables (IntelliJ or GitHub)
    private final String SLACK_WEBHOOK = System.getenv("SLACK_WEBHOOK_URL");

    public void openBrowser() {
        ChromeOptions options = new ChromeOptions();

        // 🧠 SMART TOGGLE: Check if we are running on GitHub Actions
        String isGitHub = System.getenv("GITHUB_ACTIONS");

        if (isGitHub != null && isGitHub.equals("true")) {
            System.out.println("☁️ SERVER MODE DETECTED: Running Chrome Headless");
            options.addArguments("--headless=new");
            options.addArguments("--disable-gpu");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");

            // 🛡️ ANTI-BOT BYPASS: Disguise the headless browser
            // 1. Fake a real Windows User-Agent (Removes "HeadlessChrome" from the signature)
            options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            // 2. Hide the webdriver flag from JavaScript
            options.addArguments("--disable-blink-features=AutomationControlled");
        } else {
            System.out.println("💻 LOCAL MODE DETECTED: Opening Chrome UI");
        }

        // Standard options for both
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--remote-allow-origins=*");
        // Removes the "Chrome is being controlled by automated software" banner
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        driver = new ChromeDriver(options);
        // Delete cookies to ensure a fresh session
        driver.manage().deleteAllCookies();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(15)); // Increased to 15s for slower CI servers
    }

    public void closeBrowser() { if (driver != null) driver.quit(); }
    public void navigate(String url) { driver.get(url); }

    // 🧠 SMART LOCATOR
    private By getLocator(String locatorString) {
        if (locatorString.startsWith("id=")) return By.id(locatorString.substring(3));
        if (locatorString.startsWith("name=")) return By.name(locatorString.substring(5));
        if (locatorString.startsWith("css=")) return By.cssSelector(locatorString.substring(4));
        if (locatorString.startsWith("xpath=")) return By.xpath(locatorString.substring(6));
        return By.xpath(locatorString); // Default
    }

    public void tap(String locator) { driver.findElement(getLocator(locator)).click(); }
    public void type(String text, String locator) { driver.findElement(getLocator(locator)).sendKeys(text); }

    public void verifyVisible(String locator) throws Exception {
        if (!driver.findElement(getLocator(locator)).isDisplayed()) {
            throw new Exception("Element not visible: " + locator);
        }
    }

    public void waitFor(int seconds) {
        try { Thread.sleep(seconds * 1000L); } catch (Exception e) {}
    }

    public void sendSlackNotification(String message) {
        if (SLACK_WEBHOOK == null || SLACK_WEBHOOK.trim().isEmpty()) {
            System.out.println("⚠️ No Slack Webhook found. Skipping Slack message: " + message);
            return;
        }
        try {
            URL url = new URL(SLACK_WEBHOOK);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            String jsonPayload = "{\"text\": \"" + message + "\"}";

            try(OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes("UTF-8"));
                os.flush();
            }
            conn.getResponseCode();
            System.out.println("✅ Slack message sent successfully!");
        } catch (Exception e) {
            System.err.println("❌ Failed to send Slack notification.");
            e.printStackTrace();
        }
    }
}