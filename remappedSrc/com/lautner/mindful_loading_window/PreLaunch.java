package com.lautner.mindful_loading_window;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.Optional;

public class PreLaunch implements ClientModInitializer {

    public static Optional<JFrame> frame = Optional.empty();

    public static Logger LOGGER = LoggerFactory.getLogger("loading-window");

    @Override
    public void onInitializeClient() {
        if (isMac()) {
            LOGGER.warn("Cannot open loading window on Mac due to OS limitations regarding AWT.");
            return;
        }

        try {
            JLabel memoryInfoLabel = new JLabel(); // Initialize memoryInfoLabel
            createAndShowUI(memoryInfoLabel); // Pass memoryInfoLabel to createAndShowUI
        } catch (Exception e) {
            LOGGER.error("Unable to show loading screen.", e);
        }

        // Listen for the Fabric Loader events to detect when Minecraft is fully loaded
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            if (frame.isPresent()) {
                frame.get().dispose(); // Close the loading frame when Minecraft is fully loaded
            }
        });
    }

    private void createAndShowUI(JLabel memoryInfoLabel) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        JFrame loadingFrame = new JFrame("Minecraft");
        loadingFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Prevent default close operation
        loadingFrame.setResizable(false);

        // Load the icon image using Minecraft's resource loader
        URL iconUrl = getClass().getResource("/assets/loading-icon/icon.png");
        assert iconUrl != null;
        ImageIcon icon = new ImageIcon(iconUrl);
        loadingFrame.setIconImage(icon.getImage());

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(false); // Change to determinate
        progressBar.setPreferredSize(new Dimension(356, (int) progressBar.getPreferredSize().getHeight()));

        JPanel mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setLayout(new BorderLayout());

        JLabel loadingLabel = new JLabel("Loading Mindful Optimized...");
        mainPanel.add(loadingLabel, BorderLayout.NORTH);

        // Add the progress bar
        mainPanel.add(progressBar, BorderLayout.CENTER);
        loadingFrame.setContentPane(mainPanel);
        loadingFrame.pack();
        loadingFrame.setLocationRelativeTo(null);
        loadingFrame.setVisible(true);
        frame = Optional.of(loadingFrame);

        mainPanel.add(memoryInfoLabel, BorderLayout.SOUTH);

        // Add a JTextArea to display the loading state of the Fabric mod loader
        JTextArea fabricLoadingStateTextArea = new JTextArea();
        fabricLoadingStateTextArea.setEditable(false);
        mainPanel.add(new JScrollPane(fabricLoadingStateTextArea), BorderLayout.EAST);

        // Update the memory information label
        updateMemoryInfoLabel(memoryInfoLabel);

        // Start a window listener to the frame
        loadingFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Call the onShutdown method to stop the loading process
                onShutdown();
                // Exit the Java Virtual Machine
                System.exit(0);
            }
        });

        // Start a thread to simulate loading progress
        new Thread(() -> {
            for (int i = 0; i <= 100; i++) {
                try {
                    Thread.sleep(50); // Adjusted to update more frequently
                    progressBar.setValue(i); // Update progress bar value
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
            // Now you can open Minecraft or perform further actions
        }).start();
    }

    // Method to update the memory information label
    private void updateMemoryInfoLabel(JLabel memoryInfoLabel) {
        long maxMemory = Runtime.getRuntime().maxMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        long usedMemory = totalMemory - Runtime.getRuntime().freeMemory();

        String memoryText = String.format("Memory: %dMB/%dMB", usedMemory / (1024 * 1024), maxMemory / (1024 * 1024));

        // Customize the appearance of the memoryInfoLabel
        memoryInfoLabel.setForeground(Color.GREEN);
        memoryInfoLabel.setText(memoryText);
    }

    // Stop the memory update timer on shutdown to avoid resource leaks
    public void onShutdown() {
        // No need to stop the memory update timer now since the application is shutting down
    }

    private static boolean isMac() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("mac");
    }
}
