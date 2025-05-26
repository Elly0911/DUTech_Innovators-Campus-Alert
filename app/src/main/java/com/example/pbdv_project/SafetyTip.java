package com.example.pbdv_project;

public class SafetyTip {
    private String title;
    private String instructions;
    private String emoji;
    private int colorResId;

    public SafetyTip(String title, String instructions) {
        this.title = title;
        this.instructions = instructions;

        if (title.contains(" ")) {
            String[] parts = title.split(" ", 2);
            if (parts[0].length() <= 2) {
                this.emoji = parts[0];
            } else {
                this.emoji = "";
            }
        } else {
            this.emoji = "";
        }

        // Set color based on emoji/emergency type
        if (title.contains("Fire")) {
            this.colorResId = R.color.fire_red;
        } else if (title.contains("Lockdown")) {
            this.colorResId = R.color.lockdown_maroon;
        } else if (title.contains("Bomb")) {
            this.colorResId = R.color.warning_orange;
        } else if (title.contains("Medical")) {
            this.colorResId = R.color.medical_green;
        } else if (title.contains("Electrical")) {
            this.colorResId = R.color.electrical_yellow;
        } else if (title.contains("Weather")) {
            this.colorResId = R.color.weather_blue;
        } else if (title.contains("Suspicious")) {
            this.colorResId = R.color.suspicious_purple;
        } else if (title.contains("Emergency")) {
            this.colorResId = R.color.alert_red;
        } else {
            this.colorResId = R.color.campus_blue;
        }
    }

    public String getTitle() {
        return title;
    }

    public String getInstructions() {
        return instructions;
    }

    public String getEmoji() {
        return emoji;
    }

    public int getColorResId() {
        return colorResId;
    }
}