package com.aziz.drive_it.DriveUtils.model;

public enum Frequency {
    DAILY(1),
    WEEKLY(7),
    MONTHLY(30),
    TEST(15); //use with minutes
    private int frequency;

    Frequency(int frequency) {
        this.frequency = frequency;
    }

    public int getFrequency() {
        return frequency;
    }
}
