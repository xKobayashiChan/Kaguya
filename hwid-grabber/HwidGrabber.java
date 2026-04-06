import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Enumeration;

/**
 * Kaguya Client - HWID Grabber
 * 自分のHWIDを表示してクリップボードにコピーするツール。
 * Firebase Console に手動でHWIDを登録する際に使用する。
 */
public class HwidGrabber {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(HwidGrabber::createAndShowGui);
    }

    private static void createAndShowGui() {
        String hwid = getHWID();

        JFrame frame = new JFrame("HWID Grabber - Kaguya Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(560, 160);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel label = new JLabel("Your HWID (Firebase に登録する値):");
        panel.add(label, BorderLayout.NORTH);

        JTextField hwidField = new JTextField(hwid);
        hwidField.setEditable(false);
        hwidField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        hwidField.setBackground(new Color(240, 240, 240));
        panel.add(hwidField, BorderLayout.CENTER);

        JButton copyButton = new JButton("Copy to Clipboard");
        copyButton.setPreferredSize(new Dimension(0, 36));
        copyButton.addActionListener(e -> {
            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new StringSelection(hwid), null);
            copyButton.setText("Copied!");
            copyButton.setBackground(new Color(144, 238, 144));
            Timer timer = new Timer(2000, evt -> {
                copyButton.setText("Copy to Clipboard");
                copyButton.setBackground(null);
            });
            timer.setRepeats(false);
            timer.start();
        });
        panel.add(copyButton, BorderLayout.SOUTH);

        frame.add(panel);
        frame.setVisible(true);
    }

    // ==================== HWID生成（HWIDUtilと同一ロジック）====================

    private static String getHWID() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(getMACAddress());
            sb.append(System.getProperty("os.name", "unknown"));
            sb.append(System.getProperty("os.arch", "unknown"));
            sb.append(System.getProperty("user.name", "unknown"));
            String computerName = System.getenv("COMPUTERNAME");
            if (computerName == null) computerName = System.getenv("HOSTNAME");
            if (computerName != null) sb.append(computerName);
            sb.append(Runtime.getRuntime().availableProcessors());
            return sha256(sb.toString());
        } catch (Exception e) {
            String fallback = System.getProperty("os.name", "")
                    + System.getProperty("user.name", "")
                    + System.getProperty("os.arch", "");
            return sha256(fallback);
        }
    }

    private static String getMACAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                byte[] mac = ni.getHardwareAddress();
                if (mac != null && mac.length > 0 && !ni.isLoopback() && !ni.isVirtual()) {
                    StringBuilder macStr = new StringBuilder();
                    for (byte b : mac) macStr.append(String.format("%02X", b));
                    return macStr.toString();
                }
            }
        } catch (Exception ignored) {}
        return "NO_MAC";
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}