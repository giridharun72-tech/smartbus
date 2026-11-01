package com.smartbus.dto;

public class EarningSummaryDTO {
    private double todayRevenue;
    private double weeklyRevenue;
    private double monthlyRevenue;
    private double totalRevenue;
    private long todayBookings;
    private long totalPassengers;
    private long activeBuses;
    private long runningBuses;
    private long cancelledBuses;
    private long delayedBuses;
    private long signalLostBuses;

    public EarningSummaryDTO() {}

    public double getTodayRevenue() { return todayRevenue; }
    public void setTodayRevenue(double todayRevenue) { this.todayRevenue = todayRevenue; }
    public double getWeeklyRevenue() { return weeklyRevenue; }
    public void setWeeklyRevenue(double weeklyRevenue) { this.weeklyRevenue = weeklyRevenue; }
    public double getMonthlyRevenue() { return monthlyRevenue; }
    public void setMonthlyRevenue(double monthlyRevenue) { this.monthlyRevenue = monthlyRevenue; }
    public double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }
    public long getTodayBookings() { return todayBookings; }
    public void setTodayBookings(long todayBookings) { this.todayBookings = todayBookings; }
    public long getTotalPassengers() { return totalPassengers; }
    public void setTotalPassengers(long totalPassengers) { this.totalPassengers = totalPassengers; }
    public long getActiveBuses() { return activeBuses; }
    public void setActiveBuses(long activeBuses) { this.activeBuses = activeBuses; }
    public long getRunningBuses() { return runningBuses; }
    public void setRunningBuses(long runningBuses) { this.runningBuses = runningBuses; }
    public long getCancelledBuses() { return cancelledBuses; }
    public void setCancelledBuses(long cancelledBuses) { this.cancelledBuses = cancelledBuses; }
    public long getDelayedBuses() { return delayedBuses; }
    public void setDelayedBuses(long delayedBuses) { this.delayedBuses = delayedBuses; }
    public long getSignalLostBuses() { return signalLostBuses; }
    public void setSignalLostBuses(long signalLostBuses) { this.signalLostBuses = signalLostBuses; }
}
