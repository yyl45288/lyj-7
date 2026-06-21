package com.warehouse.wavepicking.dto.response;

import java.util.List;

public class DashboardResponse {

    private DashboardStats stats;
    private List<OrderSummaryResponse> pendingOrders;
    private List<WaveSummaryResponse> activeWaves;
    private List<InventoryResponse> lowStockItems;

    public DashboardResponse() {
    }

    public DashboardResponse(DashboardStats stats, List<OrderSummaryResponse> pendingOrders, List<WaveSummaryResponse> activeWaves, List<InventoryResponse> lowStockItems) {
        this.stats = stats;
        this.pendingOrders = pendingOrders;
        this.activeWaves = activeWaves;
        this.lowStockItems = lowStockItems;
    }

    public DashboardStats getStats() {
        return stats;
    }

    public void setStats(DashboardStats stats) {
        this.stats = stats;
    }

    public List<OrderSummaryResponse> getPendingOrders() {
        return pendingOrders;
    }

    public void setPendingOrders(List<OrderSummaryResponse> pendingOrders) {
        this.pendingOrders = pendingOrders;
    }

    public List<WaveSummaryResponse> getActiveWaves() {
        return activeWaves;
    }

    public void setActiveWaves(List<WaveSummaryResponse> activeWaves) {
        this.activeWaves = activeWaves;
    }

    public List<InventoryResponse> getLowStockItems() {
        return lowStockItems;
    }

    public void setLowStockItems(List<InventoryResponse> lowStockItems) {
        this.lowStockItems = lowStockItems;
    }

    public static DashboardResponseBuilder builder() {
        return new DashboardResponseBuilder();
    }

    public static class DashboardResponseBuilder {
        private DashboardStats stats;
        private List<OrderSummaryResponse> pendingOrders;
        private List<WaveSummaryResponse> activeWaves;
        private List<InventoryResponse> lowStockItems;

        private DashboardResponseBuilder() {
        }

        public DashboardResponseBuilder stats(DashboardStats stats) {
            this.stats = stats;
            return this;
        }

        public DashboardResponseBuilder pendingOrders(List<OrderSummaryResponse> pendingOrders) {
            this.pendingOrders = pendingOrders;
            return this;
        }

        public DashboardResponseBuilder activeWaves(List<WaveSummaryResponse> activeWaves) {
            this.activeWaves = activeWaves;
            return this;
        }

        public DashboardResponseBuilder lowStockItems(List<InventoryResponse> lowStockItems) {
            this.lowStockItems = lowStockItems;
            return this;
        }

        public DashboardResponse build() {
            return new DashboardResponse(stats, pendingOrders, activeWaves, lowStockItems);
        }
    }

    public static class DashboardStats {
        private Long pendingOrderCount;
        private Long totalOrderCount;
        private Long activeWaveCount;
        private Long completedWaveCount;
        private Long pendingTaskCount;
        private Long completedTaskCount;
        private Long outOfStockSkuCount;
        private Long totalSkuCount;

        public DashboardStats() {
        }

        public DashboardStats(Long pendingOrderCount, Long totalOrderCount, Long activeWaveCount, Long completedWaveCount, Long pendingTaskCount, Long completedTaskCount, Long outOfStockSkuCount, Long totalSkuCount) {
            this.pendingOrderCount = pendingOrderCount;
            this.totalOrderCount = totalOrderCount;
            this.activeWaveCount = activeWaveCount;
            this.completedWaveCount = completedWaveCount;
            this.pendingTaskCount = pendingTaskCount;
            this.completedTaskCount = completedTaskCount;
            this.outOfStockSkuCount = outOfStockSkuCount;
            this.totalSkuCount = totalSkuCount;
        }

        public Long getPendingOrderCount() {
            return pendingOrderCount;
        }

        public void setPendingOrderCount(Long pendingOrderCount) {
            this.pendingOrderCount = pendingOrderCount;
        }

        public Long getTotalOrderCount() {
            return totalOrderCount;
        }

        public void setTotalOrderCount(Long totalOrderCount) {
            this.totalOrderCount = totalOrderCount;
        }

        public Long getActiveWaveCount() {
            return activeWaveCount;
        }

        public void setActiveWaveCount(Long activeWaveCount) {
            this.activeWaveCount = activeWaveCount;
        }

        public Long getCompletedWaveCount() {
            return completedWaveCount;
        }

        public void setCompletedWaveCount(Long completedWaveCount) {
            this.completedWaveCount = completedWaveCount;
        }

        public Long getPendingTaskCount() {
            return pendingTaskCount;
        }

        public void setPendingTaskCount(Long pendingTaskCount) {
            this.pendingTaskCount = pendingTaskCount;
        }

        public Long getCompletedTaskCount() {
            return completedTaskCount;
        }

        public void setCompletedTaskCount(Long completedTaskCount) {
            this.completedTaskCount = completedTaskCount;
        }

        public Long getOutOfStockSkuCount() {
            return outOfStockSkuCount;
        }

        public void setOutOfStockSkuCount(Long outOfStockSkuCount) {
            this.outOfStockSkuCount = outOfStockSkuCount;
        }

        public Long getTotalSkuCount() {
            return totalSkuCount;
        }

        public void setTotalSkuCount(Long totalSkuCount) {
            this.totalSkuCount = totalSkuCount;
        }

        public static DashboardStatsBuilder builder() {
            return new DashboardStatsBuilder();
        }

        public static class DashboardStatsBuilder {
            private Long pendingOrderCount;
            private Long totalOrderCount;
            private Long activeWaveCount;
            private Long completedWaveCount;
            private Long pendingTaskCount;
            private Long completedTaskCount;
            private Long outOfStockSkuCount;
            private Long totalSkuCount;

            private DashboardStatsBuilder() {
            }

            public DashboardStatsBuilder pendingOrderCount(Long pendingOrderCount) {
                this.pendingOrderCount = pendingOrderCount;
                return this;
            }

            public DashboardStatsBuilder totalOrderCount(Long totalOrderCount) {
                this.totalOrderCount = totalOrderCount;
                return this;
            }

            public DashboardStatsBuilder activeWaveCount(Long activeWaveCount) {
                this.activeWaveCount = activeWaveCount;
                return this;
            }

            public DashboardStatsBuilder completedWaveCount(Long completedWaveCount) {
                this.completedWaveCount = completedWaveCount;
                return this;
            }

            public DashboardStatsBuilder pendingTaskCount(Long pendingTaskCount) {
                this.pendingTaskCount = pendingTaskCount;
                return this;
            }

            public DashboardStatsBuilder completedTaskCount(Long completedTaskCount) {
                this.completedTaskCount = completedTaskCount;
                return this;
            }

            public DashboardStatsBuilder outOfStockSkuCount(Long outOfStockSkuCount) {
                this.outOfStockSkuCount = outOfStockSkuCount;
                return this;
            }

            public DashboardStatsBuilder totalSkuCount(Long totalSkuCount) {
                this.totalSkuCount = totalSkuCount;
                return this;
            }

            public DashboardStats build() {
                return new DashboardStats(pendingOrderCount, totalOrderCount, activeWaveCount, completedWaveCount, pendingTaskCount, completedTaskCount, outOfStockSkuCount, totalSkuCount);
            }
        }
    }

    public static class WaveSummaryResponse {
        private Long id;
        private String waveNo;
        private String status;
        private String waveType;
        private Integer orderCount;
        private Integer taskCount;
        private Integer completedTaskCount;

        public WaveSummaryResponse() {
        }

        public WaveSummaryResponse(Long id, String waveNo, String status, String waveType, Integer orderCount, Integer taskCount, Integer completedTaskCount) {
            this.id = id;
            this.waveNo = waveNo;
            this.status = status;
            this.waveType = waveType;
            this.orderCount = orderCount;
            this.taskCount = taskCount;
            this.completedTaskCount = completedTaskCount;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getWaveNo() {
            return waveNo;
        }

        public void setWaveNo(String waveNo) {
            this.waveNo = waveNo;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getWaveType() {
            return waveType;
        }

        public void setWaveType(String waveType) {
            this.waveType = waveType;
        }

        public Integer getOrderCount() {
            return orderCount;
        }

        public void setOrderCount(Integer orderCount) {
            this.orderCount = orderCount;
        }

        public Integer getTaskCount() {
            return taskCount;
        }

        public void setTaskCount(Integer taskCount) {
            this.taskCount = taskCount;
        }

        public Integer getCompletedTaskCount() {
            return completedTaskCount;
        }

        public void setCompletedTaskCount(Integer completedTaskCount) {
            this.completedTaskCount = completedTaskCount;
        }

        public static WaveSummaryResponseBuilder builder() {
            return new WaveSummaryResponseBuilder();
        }

        public static class WaveSummaryResponseBuilder {
            private Long id;
            private String waveNo;
            private String status;
            private String waveType;
            private Integer orderCount;
            private Integer taskCount;
            private Integer completedTaskCount;

            private WaveSummaryResponseBuilder() {
            }

            public WaveSummaryResponseBuilder id(Long id) {
                this.id = id;
                return this;
            }

            public WaveSummaryResponseBuilder waveNo(String waveNo) {
                this.waveNo = waveNo;
                return this;
            }

            public WaveSummaryResponseBuilder status(String status) {
                this.status = status;
                return this;
            }

            public WaveSummaryResponseBuilder waveType(String waveType) {
                this.waveType = waveType;
                return this;
            }

            public WaveSummaryResponseBuilder orderCount(Integer orderCount) {
                this.orderCount = orderCount;
                return this;
            }

            public WaveSummaryResponseBuilder taskCount(Integer taskCount) {
                this.taskCount = taskCount;
                return this;
            }

            public WaveSummaryResponseBuilder completedTaskCount(Integer completedTaskCount) {
                this.completedTaskCount = completedTaskCount;
                return this;
            }

            public WaveSummaryResponse build() {
                return new WaveSummaryResponse(id, waveNo, status, waveType, orderCount, taskCount, completedTaskCount);
            }
        }
    }
}
