package com.x.order.dto;

/** How a sales report is split: by hour (a day), by day (a week or month) or by month (a year). */
public enum ReportBucket {
    HOUR("%Y-%m-%d %H:00"),
    DAY("%Y-%m-%d"),
    MONTH("%Y-%m");

    /** MySQL DATE_FORMAT pattern whose text is both the bucket's label and its sort key. */
    private final String pattern;

    ReportBucket(String pattern) {
        this.pattern = pattern;
    }

    public String pattern() {
        return pattern;
    }
}
