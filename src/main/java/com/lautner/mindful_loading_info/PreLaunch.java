package com.lautner.mindful_loading_info;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.net.URL;
import java.util.Optional;

public class PreLaunch implements PreLaunchEntrypoint {
    public static Optional<JFrame> frame = Optional.empty();
    public static Logger LOGGER = LoggerFactory.getLogger("loading-window");
    private Timer memoryUpdateTimer;
    private boolean minecraftWindowVisible = false;

    public PreLaunch() {
    }

    private static boolean isMac() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("mac");
    }

    public void onPreLaunch() {
        if (isMac()) {
            LOGGER.warn("Cannot open loading window on Mac due to OS limitations regarding AWT.");
        } else {
            try {
                JLabel memoryInfoLabel = new JLabel();
                this.createAndShowUI(memoryInfoLabel);
                this.startMemoryUpdateTimer(memoryInfoLabel);
            } catch (Exception var2) {
                LOGGER.error("Unable to show loading screen.", var2.getMessage());
            }
        }
    }

    private void createAndShowUI(JLabel memoryInfoLabel) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        JFrame loadingFrame = new JFrame("Minecraft");
        loadingFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Prevent default close operation
        loadingFrame.setResizable(false);
        URL iconUrl = this.getClass().getResource("/assets/loading-icon/icon.png");

        assert iconUrl != null;

        ImageIcon icon = new ImageIcon(iconUrl);
        loadingFrame.setIconImage(icon.getImage());
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setPreferredSize(new Dimension(256, 40));
        JPanel mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setLayout(new BorderLayout());
        JLabel loadingLabel = new JLabel("Loading Mindful Optimized...");
        mainPanel.add(loadingLabel, BorderLayout.NORTH);
        mainPanel.add(progressBar, BorderLayout.CENTER);
        loadingFrame.setContentPane(mainPanel);
        loadingFrame.pack();
        loadingFrame.setLocationRelativeTo(null);
        loadingFrame.setVisible(true);
        frame = Optional.of(loadingFrame);
        mainPanel.add(memoryInfoLabel, BorderLayout.SOUTH);
        JTextArea fabricLoadingStateTextArea = new JTextArea();
        fabricLoadingStateTextArea.setEditable(false);
        mainPanel.add(new JScrollPane(fabricLoadingStateTextArea), BorderLayout.EAST);
        updateMemoryInfoLabel(memoryInfoLabel);

        // Listen for the Minecraft Java window visibility
        loadingFrame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                minecraftWindowVisible = true;
            }
        });

        // Start a thread to simulate loading progress
        new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                try {
                    Thread.sleep(50); // Adjusted to update more frequently
                    progressBar.setValue(i); // Update progress bar value
                } catch (InterruptedException ex) {
                    LOGGER.error("Thread sleep interrupted", ex);
                }
            }
            // Wait for the Minecraft window to become visible before closing the loading window
            while (!minecraftWindowVisible) {
                try {
                    Thread.sleep(1000); // Check every second
                } catch (InterruptedException ex) {
                    LOGGER.error("Thread sleep interrupted", ex);
                }
            }
            // Now you can close the loading window
            SwingUtilities.invokeLater(() -> loadingFrame.dispose());
        }).start();
    }

    private void updateMemoryInfoLabel(JLabel memoryInfoLabel) {
        long maxMemory = Runtime.getRuntime().maxMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        long usedMemory = totalMemory - Runtime.getRuntime().freeMemory();
        String memoryText = String.format("Memory: %dMB/%dMB", usedMemory / 1048576L, maxMemory / 1048576L);
        Color aquaColor = new Color(0,153,255);
        memoryInfoLabel.setForeground(Color.getColor(String.valueOf(aquaColor)));
        memoryInfoLabel.setText(memoryText);
    }

    private void startMemoryUpdateTimer(JLabel memoryInfoLabel) {
        this.memoryUpdateTimer = new Timer(1000, (e) -> this.updateMemoryInfoLabel(memoryInfoLabel));
        this.memoryUpdateTimer.start();
    }

    public void onShutdown() {
        if (this.memoryUpdateTimer != null && this.memoryUpdateTimer.isRunning()) {
            this.memoryUpdateTimer.stop();
        }
    }
}
