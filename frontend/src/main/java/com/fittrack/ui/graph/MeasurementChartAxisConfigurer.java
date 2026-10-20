package com.fittrack.ui.graph;

import javafx.scene.chart.NumberAxis;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class MeasurementChartAxisConfigurer {

    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    // Property keys (stored in axis.getProperties())
    private static final String POINTS_KEY = "chart.points";
    private static final String X_LAYOUT_KEY = "chart.xLayout";
    private static final String Y_LAYOUT_KEY = "chart.yLayout";
    private static final double X_EDGE_PADDING_PX = 42;

    // X axis
    private static final double X_DEFAULT_LENGTH = 600;
    private static final double X_PIXELS_PER_LABEL = 90;
    private static final int X_MAX_LABELS = 7;

    // Y axis
    private static final double Y_DEFAULT_LENGTH = 170;
    private static final double Y_PIXELS_PER_INTERVAL = 45;
    private static final double Y_PADDING_RATIO = 0.15;
    private static final double Y_MIN_SPAN = 2.0;
    private static final int Y_GRID_SUBDIVISIONS = 2;
    private static final double Y_MIN_LABEL_DISTANCE_PX = 20;

    private record XLayout(long firstDay, long span, int labelIntervals, int subdivisions, double grid) {}

    private record YLayout(double step) {}

    private MeasurementChartAxisConfigurer() {}

    public static void initialize(NumberAxis xAxis, NumberAxis yAxis) {
        xAxis.setForceZeroInRange(false);
        yAxis.setForceZeroInRange(false);

        xAxis.setAutoRanging(false);
        yAxis.setAutoRanging(false);

        xAxis.setMinorTickVisible(false);
        yAxis.setMinorTickVisible(false);

        configureXFormatter(xAxis);
        configureYFormatter(yAxis);

        // When the axis size changes, the number of labels has to be recalculated
        xAxis.widthProperty().addListener((obs, oldValue, newValue) -> {
            List<ChartPoint> points = storedPoints(xAxis);
            if (points != null) {
                configureXAxis(xAxis, points);
            }
        });

        yAxis.heightProperty().addListener((obs, oldValue, newValue) -> {
            List<ChartPoint> points = storedPoints(yAxis);
            if (points != null) {
                configureYAxis(yAxis, points);
            }
        });
    }

    public static void configure(NumberAxis xAxis, NumberAxis yAxis, List<ChartPoint> points) {
        xAxis.getProperties().put(POINTS_KEY, points);
        yAxis.getProperties().put(POINTS_KEY, points);

        configureXAxis(xAxis, points);
        configureYAxis(yAxis, points);
    }

    @SuppressWarnings("unchecked")
    private static List<ChartPoint> storedPoints(NumberAxis axis) {
        return (List<ChartPoint>) axis.getProperties().get(POINTS_KEY);
    }

    // ── X axis ──────────────────────────────────────────────────────
    private static void configureXFormatter(NumberAxis xAxis) {
        xAxis.setTickLabelFormatter(
                new StringConverter<>() {
                    @Override
                    public String toString(Number value) {
                        if (value == null) {
                            return "";
                        }

                        XLayout layout = (XLayout) xAxis.getProperties().get(X_LAYOUT_KEY);

                        if (layout == null) {
                            return "";
                        }

                        double index = (value.doubleValue() - layout.firstDay()) / layout.grid();
                        long rounded = Math.round(index);

                        boolean onGrid = Math.abs(index - rounded) < 0.01;
                        boolean inRange = rounded >= 0 && rounded <= (long) layout.labelIntervals() * layout.subdivisions();
                        boolean isLabel = rounded % layout.subdivisions() == 0;

                        if (!onGrid || !inRange || !isLabel) {
                            return "";
                        }

                        long labelIndex = rounded / layout.subdivisions();

                        long day = layout.firstDay() + Math.round(
                                (double) layout.span() * labelIndex / layout.labelIntervals()
                        );

                        LocalDate date = LocalDate.ofEpochDay(day);

                        return layout.span() > 366
                                ? date.format(LONG_DATE)
                                : date.format(SHORT_DATE);
                    }

                    @Override
                    public Number fromString(String string) {
                        return null;
                    }
                }
        );
    }

    private static void configureXAxis(NumberAxis xAxis, List<ChartPoint> points) {
        long firstDay = points.getFirst().date().toEpochDay();
        long lastDay = points.getLast().date().toEpochDay();

        long span = Math.max(1, lastDay - firstDay);

        double length = xAxis.getWidth() > 0 ? xAxis.getWidth() : X_DEFAULT_LENGTH;

        int maxLabels = (int) Math.floor(length / X_PIXELS_PER_LABEL);
        maxLabels = Math.clamp(maxLabels, 2, X_MAX_LABELS);

        int labelIntervals = Math.clamp(span, 1, maxLabels - 1);

        double labelUnit = (double) span / labelIntervals;

        double usableLength = Math.max(1, length - 2 * X_EDGE_PADDING_PX);
        double paddingDays = span * X_EDGE_PADDING_PX / usableLength;

        int subdivisions = Math.max(1, (int) Math.floor(labelUnit / paddingDays));

        double grid = labelUnit / subdivisions;

        xAxis.getProperties().put(
                X_LAYOUT_KEY,
                new XLayout(firstDay, span, labelIntervals, subdivisions, grid)
        );

        // One grid cell of empty space on each side
        xAxis.setTickUnit(grid);
        xAxis.setLowerBound(firstDay - grid);
        xAxis.setUpperBound(lastDay + grid * 1.001);
    }

    // ── Y axis ──────────────────────────────────────────────────────
    private static void configureYFormatter(NumberAxis yAxis) {
        yAxis.setTickLabelFormatter(
                new StringConverter<>() {
                    @Override
                    public String toString(Number value) {
                        if (value == null) {
                            return "";
                        }

                        YLayout layout = (YLayout) yAxis.getProperties().get(Y_LAYOUT_KEY);

                        double number = value.doubleValue();

                        if (layout != null) {
                            double index = number / layout.step();

                            if (Math.abs(index - Math.rint(index)) > 1e-4) {
                                return "";
                            }
                        }

                        if (Math.abs(number - Math.rint(number)) < 0.001) {
                            return String.format(Locale.ENGLISH, "%.0f", number);
                        }

                        return String.format(Locale.ENGLISH, "%.1f", number);
                    }

                    @Override
                    public Number fromString(String string) {
                        return null;
                    }
                }
        );
    }

    private static void configureYAxis(NumberAxis yAxis, List<ChartPoint> points) {
        double minimum = points.stream().mapToDouble(ChartPoint::value).min().orElse(0);
        double maximum = points.stream().mapToDouble(ChartPoint::value).max().orElse(0);

        double range = maximum - minimum;

        double padding = range < 1e-9
                ? Math.max(1, Math.abs(maximum) * 0.01)
                : range * Y_PADDING_RATIO;

        double lower = minimum - padding;
        double upper = maximum + padding;

        if (upper - lower < Y_MIN_SPAN) {
            double center = (lower + upper) / 2;
            lower = center - Y_MIN_SPAN / 2;
            upper = center + Y_MIN_SPAN / 2;
        }

        double length = yAxis.getHeight() > 0 ? yAxis.getHeight() : Y_DEFAULT_LENGTH;

        int intervals = (int) Math.round(length / Y_PIXELS_PER_INTERVAL);
        intervals = Math.clamp(intervals, 2, 6);

        double step = niceStep((upper - lower) / intervals);
        double grid = step / Y_GRID_SUBDIVISIONS;

        double lowerBound = Math.floor(lower / grid) * grid;
        double upperBound = Math.ceil(upper / grid) * grid;

        double minLabelDistance = Y_MIN_LABEL_DISTANCE_PX * (upper - lower) / length;

        while (true) {
            double gap = Math.ceil(lowerBound / step - 1e-9) * step - lowerBound;

            if (gap < 1e-6 || gap >= minLabelDistance) {
                break;
            }

            lowerBound -= grid;
        }

        while (true) {
            double gap = upperBound - Math.floor(upperBound / step + 1e-9) * step;

            if (gap < 1e-6 || gap >= minLabelDistance) {
                break;
            }

            upperBound += grid;
        }

        yAxis.getProperties().put(Y_LAYOUT_KEY, new YLayout(step));

        yAxis.setTickUnit(grid);
        yAxis.setLowerBound(lowerBound);
        yAxis.setUpperBound(upperBound);
    }

    private static double niceStep(double rawStep) {
        double magnitude = Math.pow(10, Math.floor(Math.log10(rawStep)));

        double[] multipliers = magnitude >= 1
                ? new double[]{1, 2, 2.5, 5, 10}
                : new double[]{1, 2, 5, 10};

        for (double multiplier : multipliers) {
            double candidate = multiplier * magnitude;

            if (candidate >= rawStep - 1e-9) {
                return candidate;
            }
        }

        return 10 * magnitude;
    }
}