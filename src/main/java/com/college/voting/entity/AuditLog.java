package com.college.voting.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String user; // "admin", student register no, or "ANONYMOUS"

    @Column(nullable = false, length = 255)
    private String action;

    @Column(nullable = false)
    private LocalDateTime time = LocalDateTime.now();

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    public AuditLog() {}

    public AuditLog(String user, String action, String ipAddress) {
        this.user = user;
        this.action = action;
        this.ipAddress = ipAddress;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}
