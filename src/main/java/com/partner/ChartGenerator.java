package com.partner;

import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.internal.chartpart.Chart;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class ChartGenerator {

    private static final String OUTPUT_DIR = "output";

    static {
        new File(OUTPUT_DIR).mkdirs();
    }

    /** 每日工单量趋势图 */
    public static String generateDailyTrend(Map<String, Integer> dailyCounts) throws IOException {
        CategoryChart chart = new CategoryChartBuilder()
                .width(800).height(400)
                .title("每日工单量趋势")
                .xAxisTitle("日期")
                .yAxisTitle("工单数")
                .build();

        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);
        chart.getStyler().setAvailableSpaceFill(0.99);
        chart.getStyler().setOverlapped(true);

        List<String> xData = new ArrayList<>(dailyCounts.keySet());
        List<Integer> yData = new ArrayList<>(dailyCounts.values());
        chart.addSeries("工单数", xData, yData);

        String path = OUTPUT_DIR + "/daily_trend.png";
        BitmapEncoder.saveBitmap(chart, path, BitmapEncoder.BitmapFormat.PNG);
        return path;
    }

    /** 分类分布饼图 */
    public static String generateCategoryPie(Map<String, Integer> categoryCounts) throws IOException {
        PieChart chart = new PieChartBuilder()
                .width(600).height(400)
                .title("问题分类分布")
                .build();

        chart.getStyler().setLegendPosition(Styler.LegendPosition.OutsideE);

        Color[] colors = {
                new Color(52, 152, 219), new Color(231, 76, 60), new Color(46, 204, 113),
                new Color(243, 156, 18), new Color(155, 89, 182), new Color(26, 188, 156)
        };
        chart.getStyler().setSeriesColors(colors);

        for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
            chart.addSeries(entry.getKey(), entry.getValue());
        }

        String path = OUTPUT_DIR + "/category_pie.png";
        BitmapEncoder.saveBitmap(chart, path, BitmapEncoder.BitmapFormat.PNG);
        return path;
    }

    /** 优先级分布柱状图 */
    public static String generatePriorityBar(Map<String, Integer> priorityCounts) throws IOException {
        CategoryChart chart = new CategoryChartBuilder()
                .width(600).height(400)
                .title("优先级分布")
                .xAxisTitle("优先级")
                .yAxisTitle("工单数")
                .build();

        chart.getStyler().setLegendVisible(false);

        List<String> xData = new ArrayList<>();
        List<Integer> yData = new ArrayList<>();
        for (String pri : new String[]{"高", "中", "低"}) {
            if (priorityCounts.containsKey(pri)) {
                xData.add(pri);
                yData.add(priorityCounts.get(pri));
            }
        }
        chart.addSeries("工单数", xData, yData);

        String path = OUTPUT_DIR + "/priority_bar.png";
        BitmapEncoder.saveBitmap(chart, path, BitmapEncoder.BitmapFormat.PNG);
        return path;
    }

    /** 各类别平均处理时长 */
    public static String generateResolutionTimeBar(Map<String, Double> catAvgTime) throws IOException {
        CategoryChart chart = new CategoryChartBuilder()
                .width(800).height(400)
                .title("各类别平均处理时长(小时)")
                .xAxisTitle("类别")
                .yAxisTitle("小时")
                .build();

        chart.getStyler().setLegendVisible(false);

        List<String> xData = new ArrayList<>(catAvgTime.keySet());
        List<Double> yData = new ArrayList<>(catAvgTime.values());
        chart.addSeries("平均时长", xData, yData);

        String path = OUTPUT_DIR + "/resolution_time.png";
        BitmapEncoder.saveBitmap(chart, path, BitmapEncoder.BitmapFormat.PNG);
        return path;
    }

    /** 各类别平均满意度 */
    public static String generateSatisfactionBar(Map<String, Double> catAvgSat) throws IOException {
        CategoryChart chart = new CategoryChartBuilder()
                .width(800).height(400)
                .title("各类别平均满意度")
                .xAxisTitle("类别")
                .yAxisTitle("满意度(1-5)")
                .build();

        chart.getStyler().setLegendVisible(false);
        chart.getStyler().setYAxisMin(0.0);
        chart.getStyler().setYAxisMax(5.0);

        List<String> xData = new ArrayList<>(catAvgSat.keySet());
        List<Double> yData = new ArrayList<>(catAvgSat.values());
        chart.addSeries("满意度", xData, yData);

        String path = OUTPUT_DIR + "/satisfaction_bar.png";
        BitmapEncoder.saveBitmap(chart, path, BitmapEncoder.BitmapFormat.PNG);
        return path;
    }

    /** 满意度评分分布 */
    public static String generateSatisfactionDistribution(Map<Integer, Integer> satDist) throws IOException {
        CategoryChart chart = new CategoryChartBuilder()
                .width(600).height(400)
                .title("满意度评分分布")
                .xAxisTitle("评分")
                .yAxisTitle("工单数")
                .build();

        chart.getStyler().setLegendVisible(false);

        List<String> xData = new ArrayList<>();
        List<Integer> yData = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            xData.add(String.valueOf(i));
            yData.add(satDist.getOrDefault(i, 0));
        }
        chart.addSeries("工单数", xData, yData);

        String path = OUTPUT_DIR + "/satisfaction_dist.png";
        BitmapEncoder.saveBitmap(chart, path, BitmapEncoder.BitmapFormat.PNG);
        return path;
    }

    /** 未解决工单分类 */
    public static String generateUnresolvedCategory(Map<String, Integer> unresolvedCat) throws IOException {
        CategoryChart chart = new CategoryChartBuilder()
                .width(600).height(400)
                .title("未解决工单分类分布")
                .xAxisTitle("类别")
                .yAxisTitle("工单数")
                .build();

        chart.getStyler().setLegendVisible(false);

        List<String> xData = new ArrayList<>(unresolvedCat.keySet());
        List<Integer> yData = new ArrayList<>(unresolvedCat.values());
        chart.addSeries("未解决", xData, yData);

        String path = OUTPUT_DIR + "/unresolved_category.png";
        BitmapEncoder.saveBitmap(chart, path, BitmapEncoder.BitmapFormat.PNG);
        return path;
    }

    /** 渠道分布 */
    public static String generateChannelBar(Map<String, Integer> channelCounts) throws IOException {
        CategoryChart chart = new CategoryChartBuilder()
                .width(600).height(400)
                .title("来源渠道分布")
                .xAxisTitle("渠道")
                .yAxisTitle("工单数")
                .build();

        chart.getStyler().setLegendVisible(false);

        List<String> xData = new ArrayList<>(channelCounts.keySet());
        List<Integer> yData = new ArrayList<>(channelCounts.values());
        chart.addSeries("工单数", xData, yData);

        String path = OUTPUT_DIR + "/channel_bar.png";
        BitmapEncoder.saveBitmap(chart, path, BitmapEncoder.BitmapFormat.PNG);
        return path;
    }

    /** 前后半段优先级对比 */
    public static String generatePeriodPriorityComparison(
            Map<String, Map<String, Integer>> periodPriority) throws IOException {
        CategoryChart chart = new CategoryChartBuilder()
                .width(700).height(400)
                .title("前后半段优先级对比")
                .xAxisTitle("优先级")
                .yAxisTitle("工单数")
                .build();

        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);

        List<String> xData = Arrays.asList("高", "中", "低");
        chart.addSeries("前半段(6/1-6/5)", xData,
                Arrays.asList(
                        periodPriority.getOrDefault("前半段", Collections.emptyMap()).getOrDefault("高", 0),
                        periodPriority.getOrDefault("前半段", Collections.emptyMap()).getOrDefault("中", 0),
                        periodPriority.getOrDefault("前半段", Collections.emptyMap()).getOrDefault("低", 0)
                ));
        chart.addSeries("后半段(6/6-6/11)", xData,
                Arrays.asList(
                        periodPriority.getOrDefault("后半段", Collections.emptyMap()).getOrDefault("高", 0),
                        periodPriority.getOrDefault("后半段", Collections.emptyMap()).getOrDefault("中", 0),
                        periodPriority.getOrDefault("后半段", Collections.emptyMap()).getOrDefault("低", 0)
                ));

        String path = OUTPUT_DIR + "/period_priority.png";
        BitmapEncoder.saveBitmap(chart, path, BitmapEncoder.BitmapFormat.PNG);
        return path;
    }
}
