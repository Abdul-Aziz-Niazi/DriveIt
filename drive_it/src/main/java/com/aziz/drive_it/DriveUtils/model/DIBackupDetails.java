package com.aziz.drive_it.DriveUtils.model;

import java.util.Date;

public class DIBackupDetails {
    private Long lastBackup;
    private Long backupSize;

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
}
