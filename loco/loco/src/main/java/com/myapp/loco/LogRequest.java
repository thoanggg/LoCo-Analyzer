package com.myapp.loco;

/**
 * POJO (Plain Old Java Object)
 * Dùng để Jackson tự động chuyển đối tượng Java này thành JSON
 * để gửi đi trong body của HTTP Request.
 */
public class LogRequest {
    private String logChannel;
    private int eventCount;
    private String xpathQuery;

    // Getters and Setters (bắt buộc cho Jackson)
    public String getLogChannel() {
        return logChannel;
    }

    public void setLogChannel(String logChannel) {
        this.logChannel = logChannel;
    }

    public int getEventCount() {
        return eventCount;
    }

    public void setEventCount(int eventCount) {
        this.eventCount = eventCount;
    }

    public String getXpathQuery() {
        return xpathQuery;
    }

    public void setXpathQuery(String xpathQuery) {
        this.xpathQuery = xpathQuery;
    }
}