package com.aziz.drive_it.DriveUtils.model;

import java.util.Date;

public class DIBackupDetails {
    private Long lastBackup = 0L;
    private Long backupSize = 0L;
    private String error;

    public Long getLastBackup() {
        return lastBackup;
    }

    public void setLastBackup(Long lastBackup) {
        this.lastBackup = lastBackup;
    }

    public Long getBackupSize() {
        return backupSize;
    }

    public void setBackupSize(Long backupSize) {
        this.backupSize = backupSize;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
