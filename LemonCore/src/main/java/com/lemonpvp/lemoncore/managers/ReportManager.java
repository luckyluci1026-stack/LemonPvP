package com.lemonpvp.lemoncore.managers;

import com.lemonpvp.lemoncore.LemonCore;
import com.lemonpvp.lemoncore.database.DatabaseManager;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ReportManager {

    public static class Report {
        public int id;
        public UUID reporterUuid;
        public String reporterName;
        public UUID reportedUuid;
        public String reportedName;
        public String reason;
        public Timestamp reportTime;
        public boolean resolved;
    }

    public static class BugReport {
        public int id;
        public UUID reporterUuid;
        public String reporterName;
        public String description;
        public Timestamp reportTime;
        public boolean resolved;
    }

    private final LemonCore plugin;
    private final DatabaseManager db;

    public ReportManager(LemonCore plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    public CompletableFuture<Void> submitReport(UUID reporter, String reporterName,
                                                  UUID reported, String reportedName, String reason) {
        return db.executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO lc_reports (reporter_uuid, reporter_name, reported_uuid, reported_name, reason) VALUES (?,?,?,?,?)")) {
                ps.setString(1, reporter.toString()); ps.setString(2, reporterName);
                ps.setString(3, reported.toString()); ps.setString(4, reportedName);
                ps.setString(5, reason);
                ps.executeUpdate();
            } catch (SQLException e) { plugin.getLogger().severe("Report error: " + e.getMessage()); }
        });
    }

    public CompletableFuture<Void> submitBugReport(UUID reporter, String reporterName, String description) {
        return db.executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO lc_bug_reports (reporter_uuid, reporter_name, description) VALUES (?,?,?)")) {
                ps.setString(1, reporter.toString()); ps.setString(2, reporterName);
                ps.setString(3, description);
                ps.executeUpdate();
            } catch (SQLException e) { plugin.getLogger().severe("BugReport error: " + e.getMessage()); }
        });
    }

    public CompletableFuture<Void> submitMessageReport(UUID reporter, String reporterName,
                                                         UUID reported, String reportedName, String reason) {
        return db.executeAsync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO lc_message_reports (reporter_uuid, reporter_name, reported_uuid, reported_name, reason) VALUES (?,?,?,?,?)")) {
                ps.setString(1, reporter.toString()); ps.setString(2, reporterName);
                ps.setString(3, reported.toString()); ps.setString(4, reportedName);
                ps.setString(5, reason);
                ps.executeUpdate();
            } catch (SQLException e) { plugin.getLogger().severe("MReport error: " + e.getMessage()); }
        });
    }

    public CompletableFuture<List<Report>> getReports() {
        return db.queryAsync(conn -> {
            List<Report> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM lc_reports WHERE resolved=FALSE ORDER BY report_time DESC LIMIT 50")) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Report r = new Report();
                    r.id = rs.getInt("id");
                    r.reporterUuid = UUID.fromString(rs.getString("reporter_uuid"));
                    r.reporterName = rs.getString("reporter_name");
                    r.reportedUuid = UUID.fromString(rs.getString("reported_uuid"));
                    r.reportedName = rs.getString("reported_name");
                    r.reason = rs.getString("reason");
                    r.reportTime = rs.getTimestamp("report_time");
                    r.resolved = rs.getBoolean("resolved");
                    list.add(r);
                }
            } catch (SQLException e) { plugin.getLogger().severe("GetReports error: " + e.getMessage()); }
            return list;
        });
    }

    public CompletableFuture<List<BugReport>> getBugReports() {
        return db.queryAsync(conn -> {
            List<BugReport> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM lc_bug_reports WHERE resolved=FALSE ORDER BY report_time DESC LIMIT 50")) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    BugReport r = new BugReport();
                    r.id = rs.getInt("id");
                    r.reporterUuid = UUID.fromString(rs.getString("reporter_uuid"));
                    r.reporterName = rs.getString("reporter_name");
                    r.description = rs.getString("description");
                    r.reportTime = rs.getTimestamp("report_time");
                    r.resolved = rs.getBoolean("resolved");
                    list.add(r);
                }
            } catch (SQLException e) { plugin.getLogger().severe("GetBugReports error: " + e.getMessage()); }
            return list;
        });
    }

    public CompletableFuture<List<Report>> getMessageReports() {
        return db.queryAsync(conn -> {
            List<Report> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM lc_message_reports WHERE resolved=FALSE ORDER BY report_time DESC LIMIT 50")) {
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Report r = new Report();
                    r.id = rs.getInt("id");
                    r.reporterUuid = UUID.fromString(rs.getString("reporter_uuid"));
                    r.reporterName = rs.getString("reporter_name");
                    r.reportedUuid = UUID.fromString(rs.getString("reported_uuid"));
                    r.reportedName = rs.getString("reported_name");
                    r.reason = rs.getString("reason");
                    r.reportTime = rs.getTimestamp("report_time");
                    r.resolved = rs.getBoolean("resolved");
                    list.add(r);
                }
            } catch (SQLException e) { plugin.getLogger().severe("GetMReports error: " + e.getMessage()); }
            return list;
        });
    }
}
