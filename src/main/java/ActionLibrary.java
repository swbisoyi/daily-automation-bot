import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

// Appium Imports
import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.screenrecording.CanRecordScreen;

// Monte Media Imports
import org.monte.media.Format;
import org.monte.media.math.Rational;
import org.monte.screenrecorder.ScreenRecorder;
import static org.monte.media.FormatKeys.*;
import static org.monte.media.VideoFormatKeys.*;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

public class ActionLibrary {
    public WebDriver driver;
    private WebDriverWait wait;
    private Properties configProps;
    private ScreenRecorder screenRecorder;

    public ActionLibrary() {
        loadConfig();
    }

    private void loadConfig() {
        configProps = new Properties();
        try (InputStream input = ActionLibrary.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) configProps.load(input);
        } catch (Exception e) {}
    }

    // ==========================================
    // 🌐 1. WEB SETUP
    // ==========================================
    public void openBrowser() {
        System.out.println("   🌐 Launching Chrome...");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--start-maximized");
        driver = new ChromeDriver(options);
        initWait();
    }

    // ==========================================
    // 🍏 2. iOS REAL DEVICE SETUP
    // ==========================================
    public void openIOSRealDevice() throws Exception {
        System.out.println("   🍏 Connecting to Real iPhone...");
        XCUITestOptions options = new XCUITestOptions();
        options.setPlatformName("iOS");
        options.setAutomationName("XCUITest");
        options.setUdid("00008140-001259423C0B001C");
        options.setDeviceName("iPhone");
        options.setBundleId("com.justdial.justdialjd");

        options.setCapability("appium:xcodeOrgId", "P6ND3CJR5D");
        options.setCapability("appium:xcodeSigningId", "iPhone Developer");
        options.setCapability("appium:updatedWDABundleId", "com.swagat.WebDriverAgentRunner");
        options.setCapability("appium:usePrebuiltWDA", true);
        options.setCapability("appium:wdaLocalPort", 8102);
        options.setNoReset(true);
        options.setNewCommandTimeout(Duration.ofSeconds(60));

        driver = new IOSDriver(new URL("http://127.0.0.1:4723/"), options);
        initWait();
    }

    // ==========================================
    // 🤖 3a. ANDROID EMULATOR SETUP
    // ==========================================
    public void openAndroidEmulator() throws Exception {
        System.out.println("   🤖 Connecting to Android Emulator...");
        UiAutomator2Options options = new UiAutomator2Options();
        options.setPlatformName("Android");
        options.setAutomationName("UiAutomator2");
        options.setDeviceName("emulator-5554");

        options.setApp("/Users/swagatkumarbisoyi/Desktop/JustdialAndroid.apk");

        // Emulator PIN logic
        options.setUnlockType("pin");
        options.setUnlockKey("1234");

        options.setAutoGrantPermissions(true);
        options.setNoReset(false);
        options.setNewCommandTimeout(Duration.ofSeconds(60));

        driver = new AndroidDriver(new URL("http://127.0.0.1:4723/"), options);
        initWait();
    }

    // ==========================================
    // 📱 3b. ANDROID REAL DEVICE SETUP
    // ==========================================
    public void openAndroidRealDevice() throws Exception {
        System.out.println("   📱 Connecting to Physical Android Device...");
        UiAutomator2Options options = new UiAutomator2Options();
        options.setPlatformName("Android");
        options.setAutomationName("UiAutomator2");
        options.setDeviceName("Android Device");

        options.setApp("/Users/swagatkumarbisoyi/Desktop/JustdialAndroid.apk");

        // 🎯 EXPLICITLY DEFINE THE PACKAGE & ACTIVITY
        options.setAppPackage("com.justdial.search");
        options.setAppActivity("com.justdial.search.SplashScreenNewActivity");

        // 🚀 Tell Appium to accept ANY Justdial screen that successfully loads
        options.setAppWaitActivity("com.justdial.search.*");

        options.setAutoGrantPermissions(true);
        options.setNoReset(true);
        options.setNewCommandTimeout(Duration.ofSeconds(60));

        driver = new AndroidDriver(new URL("http://127.0.0.1:4723/"), options);
        initWait();
        System.out.println("   🚀 Android App Launched on Real Device!");
    }

    // ==========================================
    // 🎥 4. VIDEO RECORDING (Optimized for Mac)
    // ==========================================
    public void startRecording() {
        try {
            if (driver instanceof CanRecordScreen) {
                System.out.println("   🎥 Started Mobile Screen Recording...");
                ((CanRecordScreen) driver).startRecordingScreen();
            } else {
                System.out.println("   🎥 Started Web Desktop Recording...");
                GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
                File targetFolder = new File("target/");
                if (!targetFolder.exists()) targetFolder.mkdirs();

                Format fileFormat = new Format(MediaTypeKey, MediaType.FILE, MimeTypeKey, MIME_AVI);
                Format screenFormat = new Format(MediaTypeKey, MediaType.VIDEO, EncodingKey, ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE,
                        CompressorNameKey, ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE, DepthKey, 24, FrameRateKey, Rational.valueOf(15),
                        QualityKey, 1.0f, KeyFrameIntervalKey, 15 * 60);
                Format mouseFormat = new Format(MediaTypeKey, MediaType.VIDEO, EncodingKey, "black", FrameRateKey, Rational.valueOf(30));

                screenRecorder = new ScreenRecorder(gc, gc.getBounds(), fileFormat, screenFormat, mouseFormat, null, targetFolder);
                screenRecorder.start();
            }
        } catch (Exception e) {
            System.err.println("   ❌ Failed to start recording: " + e.getMessage());
        }
    }

    public void stopRecording(String scenarioName) {
        try {
            if (driver instanceof CanRecordScreen) {
                System.out.println("   💾 Stopping Mobile recording...");
                String base64Video = ((CanRecordScreen) driver).stopRecordingScreen();
                byte[] decodedVideo = Base64.getDecoder().decode(base64Video);

                String baseName = scenarioName.replace(".txt", "") + "_Mobile_Video";
                File rawFile = new File("target/" + baseName + "_raw.mp4");
                File finalFile = new File("target/" + baseName + ".mp4");

                Files.write(rawFile.toPath(), decodedVideo);

                System.out.println("   🔄 Optimizing Mobile Video for QuickTime...");
                ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-y", "-i", rawFile.getAbsolutePath(), "-c:v", "libx264", "-pix_fmt", "yuv420p", "-preset", "fast", finalFile.getAbsolutePath());
                pb.redirectErrorStream(true).start().waitFor();

                if (finalFile.exists()) {
                    rawFile.delete();
                    System.out.println("   ✅ Mobile Video saved: " + finalFile.getAbsolutePath());
                }
            } else if (screenRecorder != null) {
                System.out.println("   💾 Stopping Web recording...");
                screenRecorder.stop();
                List<File> createdFiles = screenRecorder.getCreatedMovieFiles();
                if (!createdFiles.isEmpty()) {
                    File aviFile = new File("target/" + scenarioName.replace(".txt", "") + "_WEB_Video.avi");
                    File mp4File = new File("target/" + scenarioName.replace(".txt", "") + "_WEB_Video.mp4");

                    if (aviFile.exists()) aviFile.delete();
                    createdFiles.get(0).renameTo(aviFile);

                    ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-y", "-i", aviFile.getAbsolutePath(), "-c:v", "libx264", "-pix_fmt", "yuv420p", "-preset", "fast", mp4File.getAbsolutePath());
                    pb.redirectErrorStream(true).start().waitFor();

                    if (mp4File.exists()) {
                        aviFile.delete();
                        System.out.println("   ✅ Web Video saved: " + mp4File.getAbsolutePath());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("   ❌ Failed to save video: " + e.getMessage());
        }
    }

    // ==========================================
    // ⚙️ CORE ACTIONS
    // ==========================================
    private void initWait() {
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    public void quit() { if (driver != null) { driver.quit(); driver = null; } }
    public void navigate(String url) { driver.get(url); }
    public void tap(String loc) { wait.until(ExpectedConditions.elementToBeClickable(getLocator(loc))).click(); }
    public void type(String txt, String loc) { WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(getLocator(loc))); el.clear(); el.sendKeys(txt); }
    public void verifyVisible(String loc) { wait.until(ExpectedConditions.visibilityOfElementLocated(getLocator(loc))); }
    public void waitFor(int seconds) { try { Thread.sleep(seconds * 1000L); } catch (Exception e) {} }

    private By getLocator(String raw) {
        if (raw.startsWith("id=")) return By.id(raw.substring(3));
        if (raw.startsWith("xpath=")) return By.xpath(raw.substring(6));
        if (raw.startsWith("accessId=")) return AppiumBy.accessibilityId(raw.substring(9));
        return By.xpath(raw);
    }

    // ==========================================
    // 🔔 SLACK NOTIFICATION
    // ==========================================
    public void sendSlackNotification(String message) {
        String webhookUrl = (configProps != null) ? configProps.getProperty("slack.webhook.url") : null;
        if (webhookUrl == null || webhookUrl.isEmpty()) webhookUrl = System.getenv("SLACK_WEBHOOK_URL");
        if (webhookUrl == null || webhookUrl.isEmpty()) return;

        try {
            String jsonPayload = "{\"text\": \"" + message.replace("\"", "'").replace("\n", "\\n") + "\"}";
            URL url = new URL(webhookUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true); conn.setRequestMethod("POST"); conn.setRequestProperty("Content-Type", "application/json");
            try (OutputStream os = conn.getOutputStream()) { os.write(jsonPayload.getBytes("UTF-8")); }
        } catch (Exception e) {}
    }
}