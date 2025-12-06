package com.myapp.locoagent;

public class LogRequest {
    private String logChannel;

    // Chỉ còn lại Channel, vì mặc định là lấy ALL và không lọc XPath
    public String getLogChannel() {
        return logChannel;
    }

    public void setLogChannel(String logChannel) {
        this.logChannel = logChannel;
    }
}