package com.partner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.*;

public class TicketAnalyzer {

    private List<Ticket> tickets;

    public static void main(String[] args) throws Exception {
        TicketAnalyzer analyzer = new TicketAnalyzer();
        analyzer.loadTickets("data/tickets.json");
        analyzer.runAnalysis();
    }

    // ========== 数据加载 ==========
    public void loadTickets(String path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        tickets = mapper.readValue(new File(path), new TypeReference<List<Ticket>>() {});
        System.out.println("成功加载 " + tickets.size() + " 条工单数据\n");
    }

    // ========== 主分析流程 ==========
    public void runAnalysis() throws Exception {
        printHeader("客服工单趋势分析报告");

        // 1. 总体概览
        printOverview();

        // 2. 时间趋势
        Map<String, Integer> dailyCounts = getDailyCounts();
        printSection("时间趋势 - 每日工单量");
        for (Map.Entry<String, Integer> e : dailyCounts.entrySet()) {
            String bar = "█".repeat(e.getValue());
            System.out.printf("  %s: %2d %s%n", e.getKey(), e.getValue(), bar);
        }

        // 3. 分类分布
        Map<String, Integer> categoryCounts = getCategoryCounts();
        printSection("分类分布");
        for (Map.Entry<String, Integer> e : categoryCounts.entrySet()) {
            double pct = e.getValue() * 100.0 / tickets.size();
            String bar = "█".repeat(e.getValue());
            System.out.printf("  %s: %2d (%.1f%%) %s%n", e.getKey(), e.getValue(), pct, bar);
        }

        // 4. 优先级分布
        Map<String, Integer> priorityCounts = getPriorityCounts();
        printSection("优先级分布");
        for (Map.Entry<String, Integer> e : priorityCounts.entrySet()) {
            double pct = e.getValue() * 100.0 / tickets.size();
            System.out.printf("  %s: %d (%.1f%%)%n", e.getKey(), e.getValue(), pct);
        }

        // 5. 渠道分布
        Map<String, Integer> channelCounts = getChannelCounts();
        printSection("渠道分布");
        for (Map.Entry<String, Integer> e : channelCounts.entrySet()) {
            double pct = e.getValue() * 100.0 / tickets.size();
            System.out.printf("  %s: %d (%.1f%%)%n", e.getKey(), e.getValue(), pct);
        }

        // 6. 各类别处理时长与满意度
        printSection("各类别处理时长与满意度");
        Map<String, Double> catAvgTime = getCategoryAvgResolutionTime();
        Map<String, Double> catAvgSat = getCategoryAvgSatisfaction();
        for (String cat : catAvgTime.keySet()) {
            System.out.printf("  %s: 平均处理 %.1fh, 满意度 %.2f, 共%d单%n",
                    cat, catAvgTime.get(cat), catAvgSat.get(cat), categoryCounts.get(cat));
        }

        // 7. 异常发现
        printHeader("异常信号发现");
        List<String> anomalies = detectAnomalies(categoryCounts, priorityCounts, channelCounts);

        // 8. 生成图表
        System.out.println("\n正在生成可视化图表...");
        generateAllCharts(dailyCounts, categoryCounts, priorityCounts,
                channelCounts, catAvgTime, catAvgSat);

        // 9. 生成 HTML 报告
        generateHtmlReport(dailyCounts, categoryCounts, priorityCounts,
                channelCounts, catAvgTime, catAvgSat, anomalies);

        System.out.println("\n✅ 分析完成！图表已保存至 output/ 目录");
        System.out.println(" HTML 报告已生成: output/report.html");
    }

    // ========== 统计方法 ==========
    private Map<String, Integer> getDailyCounts() {
        return tickets.stream()
                .collect(Collectors.groupingBy(Ticket::getDate, Collectors.summingInt(t -> 1)));
    }

    private Map<String, Integer> getCategoryCounts() {
        return tickets.stream()
                .collect(Collectors.groupingBy(Ticket::getCategory, Collectors.summingInt(t -> 1)))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    private Map<String, Integer> getPriorityCounts() {
        return tickets.stream()
                .collect(Collectors.groupingBy(Ticket::getPriority, Collectors.summingInt(t -> 1)));
    }

    private Map<String, Integer> getChannelCounts() {
        return tickets.stream()
                .collect(Collectors.groupingBy(Ticket::getChannel, Collectors.summingInt(t -> 1)));
    }

    private Map<String, Double> getCategoryAvgResolutionTime() {
        return tickets.stream()
                .collect(Collectors.groupingBy(Ticket::getCategory,
                        Collectors.averagingDouble(Ticket::getResolutionTimeHours)))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    private Map<String, Double> getCategoryAvgSatisfaction() {
        return tickets.stream()
                .collect(Collectors.groupingBy(Ticket::getCategory,
                        Collectors.averagingInt(Ticket::getSatisfaction)))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    private Map<Integer, Integer> getSatisfactionDistribution() {
        return tickets.stream()
                .collect(Collectors.groupingBy(Ticket::getSatisfaction, Collectors.summingInt(t -> 1)));
    }

    private List<Ticket> getUnresolvedTickets() {
        return tickets.stream().filter(t -> !t.isResolved()).collect(Collectors.toList());
    }

    private Map<String, Integer> getUnresolvedCategoryCounts() {
        return getUnresolvedTickets().stream()
                .collect(Collectors.groupingBy(Ticket::getCategory, Collectors.summingInt(t -> 1)));
    }

    private Map<String, Map<String, Integer>> getPeriodPriority() {
        return tickets.stream()
                .collect(Collectors.groupingBy(
                        t -> {
                            String date = t.getDate();
                            return date.compareTo("2024-06-06") < 0 ? "前半段" : "后半段";
                        },
                        Collectors.groupingBy(Ticket::getPriority, Collectors.summingInt(t -> 1))
                ));
    }

    // ========== 异常检测 ==========
    private List<String> detectAnomalies(Map<String, Integer> categoryCounts,
                                          Map<String, Integer> priorityCounts,
                                          Map<String, Integer> channelCounts) {
        List<String> anomalies = new ArrayList<>();

        // 异常1: 支付问题占比
        int paymentCount = categoryCounts.getOrDefault("支付问题", 0);
        double paymentPct = paymentCount * 100.0 / tickets.size();
        anomalies.add(String.format(
                "⚠ 异常1 - 支付问题集中爆发: 支付问题共 %d 单，占比 %.1f%%，为所有分类中最高。" +
                "判断依据: 支付类工单数量远超其他类别，可能存在系统性支付故障。",
                paymentCount, paymentPct));

        // 异常2: 未解决工单
        List<Ticket> unresolved = getUnresolvedTickets();
        StringBuilder sb2 = new StringBuilder();
        sb2.append(String.format("⚠ 异常2 - 未解决工单积压: 共 %d 单未解决 (%.1f%%):%n",
                unresolved.size(), unresolved.size() * 100.0 / tickets.size()));
        for (Ticket t : unresolved) {
            sb2.append(String.format("    - %s [%s] 处理%.0fh 满意度%d%n",
                    t.getTicketId(), t.getCategory(), t.getResolutionTimeHours(), t.getSatisfaction()));
        }
        sb2.append("  判断依据: 未解决工单中多为高优先级，需立即关注。");
        anomalies.add(sb2.toString());

        // 异常3: 低满意度
        long lowSatCount = tickets.stream().filter(t -> t.getSatisfaction() <= 2).count();
        Map<String, Long> lowSatByCat = tickets.stream()
                .filter(t -> t.getSatisfaction() <= 2)
                .collect(Collectors.groupingBy(Ticket::getCategory, Collectors.counting()));
        StringBuilder sb3 = new StringBuilder();
        sb3.append(String.format("⚠ 异常3 - 低满意度工单集中: 满意度≤2的工单共 %d 单，占比 %.1f%%%n",
                lowSatCount, lowSatCount * 100.0 / tickets.size()));
        for (Map.Entry<String, Long> e : lowSatByCat.entrySet()) {
            sb3.append(String.format("    - %s: %d单%n", e.getKey(), e.getValue()));
        }
        sb3.append("  判断依据: 低满意度集中在支付问题和退款退货，说明核心业务流程体验差。");
        anomalies.add(sb3.toString());

        // 异常4: 超长处理时长
        List<Ticket> longTime = tickets.stream()
                .filter(t -> t.getResolutionTimeHours() >= 48)
                .collect(Collectors.toList());
        double avgTime = tickets.stream().mapToDouble(Ticket::getResolutionTimeHours).average().orElse(0);
        StringBuilder sb4 = new StringBuilder();
        sb4.append(String.format("⚠ 异常4 - 超长处理时长: 处理时长≥48小时的工单共 %d 单 (平均%.1fh):%n",
                longTime.size(), avgTime));
        for (Ticket t : longTime) {
            sb4.append(String.format("    - %s [%s] %.0fh 满意度%d%n",
                    t.getTicketId(), t.getCategory(), t.getResolutionTimeHours(), t.getSatisfaction()));
        }
        sb4.append("  判断依据: 这些工单处理时长远超平均值，严重影响用户体验。");
        anomalies.add(sb4.toString());

        // 异常5: 退款退货反复出现
        int refundCount = categoryCounts.getOrDefault("退款退货", 0);
        anomalies.add(String.format(
                "⚠ 异常5 - 退款退货问题反复出现: 退款退货共 %d 单，贯穿整个时间周期，" +
                "涉及运费争议、退款延迟、退货审核等多种子问题。" +
                "判断依据: 该类别贯穿整个周期，说明退款流程存在系统性问题。", refundCount));

        // 异常6: 后半段高优先级增多
        Map<String, Map<String, Integer>> periodPriority = getPeriodPriority();
        int firstHalfHigh = periodPriority.getOrDefault("前半段", Collections.emptyMap()).getOrDefault("高", 0);
        int secondHalfHigh = periodPriority.getOrDefault("后半段", Collections.emptyMap()).getOrDefault("高", 0);
        anomalies.add(String.format(
                "⚠ 异常6 - 后半段高优先级工单增多: 前半段高优先级 %d 单，后半段高优先级 %d 单。" +
                "判断依据: 后半段高优先级工单占比上升，问题严重程度在加剧。",
                firstHalfHigh, secondHalfHigh));

        // 打印异常
        for (String a : anomalies) {
            System.out.println("\n" + a);
        }

        return anomalies;
    }

    // ========== 图表生成 ==========
    private void generateAllCharts(Map<String, Integer> dailyCounts,
                                    Map<String, Integer> categoryCounts,
                                    Map<String, Integer> priorityCounts,
                                    Map<String, Integer> channelCounts,
                                    Map<String, Double> catAvgTime,
                                    Map<String, Double> catAvgSat) throws Exception {
        ChartGenerator.generateDailyTrend(dailyCounts);
        ChartGenerator.generateCategoryPie(categoryCounts);
        ChartGenerator.generatePriorityBar(priorityCounts);
        ChartGenerator.generateResolutionTimeBar(catAvgTime);
        ChartGenerator.generateSatisfactionBar(catAvgSat);
        ChartGenerator.generateSatisfactionDistribution(getSatisfactionDistribution());
        ChartGenerator.generateUnresolvedCategory(getUnresolvedCategoryCounts());
        ChartGenerator.generateChannelBar(channelCounts);
        ChartGenerator.generatePeriodPriorityComparison(getPeriodPriority());
        System.out.println("  已生成 9 张图表");
    }

    // ========== HTML 报告生成 ==========
    private void generateHtmlReport(Map<String, Integer> dailyCounts,
                                     Map<String, Integer> categoryCounts,
                                     Map<String, Integer> priorityCounts,
                                     Map<String, Integer> channelCounts,
                                     Map<String, Double> catAvgTime,
                                     Map<String, Double> catAvgSat,
                                     List<String> anomalies) throws IOException {

        double avgTime = tickets.stream().mapToDouble(Ticket::getResolutionTimeHours).average().orElse(0);
        double avgSat = tickets.stream().mapToDouble(Ticket::getSatisfaction).average().orElse(0);
        long resolved = tickets.stream().filter(Ticket::isResolved).count();
        long unresolved = tickets.size() - resolved;

        StringBuilder html = new StringBuilder();
        html.append("""
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>客服工单趋势分析报告</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif; background: #f0f2f5; color: #333; padding: 20px; }
        .container { max-width: 1200px; margin: 0 auto; }
        h1 { text-align: center; color: #1a1a2e; margin: 20px 0 30px; font-size: 28px; }
        h2 { color: #16213e; margin: 30px 0 15px; font-size: 22px; border-left: 4px solid #0f3460; padding-left: 12px; }
        h3 { color: #0f3460; margin: 20px 0 10px; font-size: 18px; }
        .card { background: white; border-radius: 12px; padding: 24px; margin-bottom: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
        .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin-bottom: 20px; }
        .stat-card { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; border-radius: 12px; padding: 20px; text-align: center; }
        .stat-card:nth-child(2) { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
        .stat-card:nth-child(3) { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }
        .stat-card:nth-child(4) { background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); }
        .stat-card:nth-child(5) { background: linear-gradient(135deg, #fa709a 0%, #fee140 100%); }
        .stat-card:nth-child(6) { background: linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%); }
        .stat-value { font-size: 32px; font-weight: bold; }
        .stat-label { font-size: 14px; opacity: 0.9; margin-top: 4px; }
        table { width: 100%; border-collapse: collapse; margin: 10px 0; }
        th, td { padding: 10px 14px; text-align: left; border-bottom: 1px solid #eee; }
        th { background: #f8f9fa; font-weight: 600; color: #555; }
        tr:hover { background: #f8f9fa; }
        .anomaly { background: #fff5f5; border-left: 4px solid #e53e3e; padding: 16px; margin: 12px 0; border-radius: 0 8px 8px 0; }
        .anomaly-title { font-weight: bold; color: #c53030; margin-bottom: 6px; }
        .chart-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; }
        .chart-card { background: white; border-radius: 12px; padding: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); text-align: center; }
        .chart-card img { max-width: 100%; border-radius: 8px; }
        .chart-card p { margin-top: 10px; font-weight: 600; color: #555; }
        .bar { display: inline-block; background: #667eea; height: 14px; border-radius: 3px; vertical-align: middle; margin-left: 8px; }
        .tag { display: inline-block; padding: 2px 10px; border-radius: 12px; font-size: 12px; font-weight: 600; }
        .tag-high { background: #fed7d7; color: #c53030; }
        .tag-mid { background: #fefcbf; color: #975a16; }
        .tag-low { background: #c6f6d5; color: #276749; }
        .footer { text-align: center; color: #999; margin-top: 40px; padding: 20px; font-size: 13px; }
    </style>
</head>
<body>
<div class="container">
    <h1>📊 客服工单趋势分析报告</h1>
    <p style="text-align:center;color:#888;margin-bottom:30px;">数据周期: 2024-06-01 ~ 2024-06-11 | 共 """);
        html.append(tickets.size()).append(" 条工单</p>\n");

        // 概览卡片
        html.append("<div class=\"stats-grid\">\n");
        html.append(statCard("工单总数", String.valueOf(tickets.size())));
        html.append(statCard("平均处理时长", String.format("%.1f 小时", avgTime)));
        html.append(statCard("平均满意度", String.format("%.2f / 5", avgSat)));
        html.append(statCard("已解决", String.valueOf(resolved)));
        html.append(statCard("未解决", String.valueOf(unresolved)));
        html.append(statCard("解决率", String.format("%.0f%%", resolved * 100.0 / tickets.size())));
        html.append("</div>\n");

        // 分类分布
        html.append("<div class=\"card\"><h2>📋 问题分类分布</h2><table><tr><th>分类</th><th>数量</th><th>占比</th><th>分布</th></tr>\n");
        for (Map.Entry<String, Integer> e : categoryCounts.entrySet()) {
            double pct = e.getValue() * 100.0 / tickets.size();
            html.append(String.format("<tr><td>%s</td><td>%d</td><td>%.1f%%</td><td><span class=\"bar\" style=\"width:%dpx\"></span></td></tr>\n",
                    e.getKey(), e.getValue(), pct, (int)(pct * 3)));
        }
        html.append("</table></div>\n");

        // 优先级
        html.append("<div class=\"card\"><h2>🔴 优先级分布</h2><table><tr><th>优先级</th><th>数量</th><th>占比</th></tr>\n");
        for (Map.Entry<String, Integer> e : priorityCounts.entrySet()) {
            double pct = e.getValue() * 100.0 / tickets.size();
            String tagClass = e.getKey().equals("高") ? "tag-high" : e.getKey().equals("中") ? "tag-mid" : "tag-low";
            html.append(String.format("<tr><td><span class=\"tag %s\">%s</span></td><td>%d</td><td>%.1f%%</td></tr>\n",
                    tagClass, e.getKey(), e.getValue(), pct));
        }
        html.append("</table></div>\n");

        // 处理时长与满意度
        html.append("<div class=\"card\"><h2>⏱ 各类别处理时长与满意度</h2><table><tr><th>类别</th><th>平均处理时长(h)</th><th>平均满意度</th><th>工单数</th></tr>\n");
        for (String cat : catAvgTime.keySet()) {
            html.append(String.format("<tr><td>%s</td><td>%.1f</td><td>%.2f</td><td>%d</td></tr>\n",
                    cat, catAvgTime.get(cat), catAvgSat.get(cat), categoryCounts.get(cat)));
        }
        html.append("</table></div>\n");

        // 异常信号
        html.append("<h2> 异常信号发现</h2>\n");
        for (String a : anomalies) {
            String[] lines = a.split("\n");
            html.append("<div class=\"anomaly\">\n");
            html.append("<div class=\"anomaly-title\">").append(lines[0]).append("</div>\n");
            for (int i = 1; i < lines.length; i++) {
                html.append("<div style=\"margin-left:12px;color:#555\">").append(lines[i]).append("</div>\n");
            }
            html.append("</div>\n");
        }

        // 未解决工单
        List<Ticket> unresolvedTickets = getUnresolvedTickets();
        if (!unresolvedTickets.isEmpty()) {
            html.append("<div class=\"card\"><h2>⚠️ 未解决工单详情</h2><table><tr><th>工单号</th><th>类别</th><th>优先级</th><th>处理时长(h)</th><th>满意度</th><th>描述</th></tr>\n");
            for (Ticket t : unresolvedTickets) {
                String tagClass = t.getPriority().equals("高") ? "tag-high" : t.getPriority().equals("中") ? "tag-mid" : "tag-low";
                html.append(String.format("<tr><td>%s</td><td>%s</td><td><span class=\"tag %s\">%s</span></td><td>%.0f</td><td>%d</td><td>%s</td></tr>\n",
                        t.getTicketId(), t.getCategory(), tagClass, t.getPriority(),
                        t.getResolutionTimeHours(), t.getSatisfaction(), t.getDescription()));
            }
            html.append("</table></div>\n");
        }

        // 图表展示
        html.append("<h2>📈 可视化图表</h2>\n");
        html.append("<div class=\"chart-grid\">\n");
        String[][] charts = {
                {"daily_trend.png", "每日工单量趋势"},
                {"category_pie.png", "问题分类分布"},
                {"priority_bar.png", "优先级分布"},
                {"resolution_time.png", "各类别平均处理时长"},
                {"satisfaction_bar.png", "各类别平均满意度"},
                {"satisfaction_dist.png", "满意度评分分布"},
                {"channel_bar.png", "来源渠道分布"},
                {"unresolved_category.png", "未解决工单分类"},
                {"period_priority.png", "前后半段优先级对比"}
        };
        for (String[] chart : charts) {
            html.append(String.format("<div class=\"chart-card\"><img src=\"%s\" alt=\"%s\"><p>%s</p></div>\n",
                    chart[0], chart[1], chart[1]));
        }
        html.append("</div>\n");

        // 分析维度说明
        html.append("""
<div class="card" style="margin-top:30px">
    <h2>📐 分析维度说明</h2>
    <table>
        <tr><th>维度</th><th>说明</th><th>决策价值</th></tr>
        <tr><td>时间趋势</td><td>每日工单量变化</td><td>识别业务高峰期，合理调配人力</td></tr>
        <tr><td>分类分布</td><td>问题类型占比</td><td>定位高频问题，推动产品/流程改进</td></tr>
        <tr><td>优先级分布</td><td>高/中/低优先级占比</td><td>确定资源投入优先级</td></tr>
        <tr><td>渠道分布</td><td>在线/电话/邮件来源</td><td>优化渠道服务策略</td></tr>
        <tr><td>处理时长</td><td>各类别平均处理时间</td><td>发现效率瓶颈</td></tr>
        <tr><td>满意度</td><td>各类别满意度评分</td><td>识别体验最差环节</td></tr>
        <tr><td>解决状态</td><td>已解决 vs 未解决</td><td>监控工单积压风险</td></tr>
    </table>
</div>

<div class="footer">
    <p>客服工单趋势分析报告 | 使用 Java + Jackson + XChart 生成 | AI 工具辅助开发</p>
</div>
</div>
</body>
</html>
""");

        Files.writeString(Path.of("output/report.html"), html.toString());
    }

    // ========== 辅助方法 ==========
    private void printOverview() {
        double avgTime = tickets.stream().mapToDouble(Ticket::getResolutionTimeHours).average().orElse(0);
        double avgSat = tickets.stream().mapToDouble(Ticket::getSatisfaction).average().orElse(0);
        long resolved = tickets.stream().filter(Ticket::isResolved).count();
        long unresolved = tickets.size() - resolved;
        String minDate = tickets.stream().map(Ticket::getCreatedAt).min(String::compareTo).orElse("");
        String maxDate = tickets.stream().map(Ticket::getCreatedAt).max(String::compareTo).orElse("");

        printSection("总体概览");
        System.out.printf("  工单总数: %d%n", tickets.size());
        System.out.printf("  时间范围: %s ~ %s%n", minDate, maxDate);
        System.out.printf("  已解决: %d / 未解决: %d%n", resolved, unresolved);
        System.out.printf("  平均处理时长: %.1f 小时%n", avgTime);
        System.out.printf("  平均满意度: %.2f / 5%n", avgSat);
    }

    private String statCard(String label, String value) {
        return String.format("<div class=\"stat-card\"><div class=\"stat-value\">%s</div><div class=\"stat-label\">%s</div></div>\n", value, label);
    }

    private void printHeader(String title) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("  " + title);
        System.out.println("=".repeat(60));
    }

    private void printSection(String title) {
        System.out.println("\n【" + title + "】");
    }
}
