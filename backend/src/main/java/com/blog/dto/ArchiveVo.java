package com.blog.dto;

import lombok.Data;
import java.util.List;

@Data
public class ArchiveVo {

    private String year;

    private List<MonthGroup> months;

    @Data
    public static class MonthGroup {
        private String month;
        private List<ArchiveItem> articles;
    }

    @Data
    public static class ArchiveItem {
        private Long id;
        private String title;
        private String createTime;
    }
}
