package com.acme.questionnaire.excel;

import org.apache.poi.ss.usermodel.DateUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

public final class ExcelCellParser {
    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    );
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.BASIC_ISO_DATE
    );

    private ExcelCellParser() {
    }

    public static String cell(Map<Integer, String> row, int index) {
        String value = row.get(index);
        return value == null ? null : value.trim();
    }

    public static String requiredText(Map<Integer, String> row,
                                      int index,
                                      String columnName,
                                      int maxLength) {
        String value = nullableText(row, index, columnName, maxLength);
        if (value == null) {
            throw new IllegalArgumentException(columnName + "不能为空");
        }
        return value;
    }

    public static String nullableText(Map<Integer, String> row,
                                      int index,
                                      String columnName,
                                      int maxLength) {
        String value = cell(row, index);
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(
                    columnName + "长度不能超过" + maxLength + "个字符");
        }
        return value;
    }

    public static Integer requiredScore(Map<Integer, String> row,
                                        int index,
                                        String columnName) {
        Integer value = nullableScore(row, index, columnName);
        if (value == null) {
            throw new IllegalArgumentException(columnName + "不能为空");
        }
        return value;
    }

    public static Integer nullableScore(Map<Integer, String> row,
                                        int index,
                                        String columnName) {
        String value = cell(row, index);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            BigDecimal number = new BigDecimal(value).stripTrailingZeros();
            if (number.scale() > 0) {
                throw new IllegalArgumentException(columnName + "必须是1-10的整数");
            }
            int score = number.intValueExact();
            if (score < 1 || score > 10) {
                throw new IllegalArgumentException(columnName + "必须在1-10之间");
            }
            return score;
        } catch (NumberFormatException | ArithmeticException ex) {
            throw new IllegalArgumentException(columnName + "必须是1-10的整数");
        }
    }

    public static LocalDateTime requiredDateTime(Map<Integer, String> row,
                                                  int index,
                                                  String columnName) {
        String value = cell(row, index);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(columnName + "不能为空");
        }

        String normalized = value.replace('T', ' ');
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
                // 尝试下一个格式
            }
        }
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(normalized, formatter).atStartOfDay();
            } catch (DateTimeParseException ignored) {
                // 尝试 Excel 日期序号
            }
        }

        try {
            double serial = new BigDecimal(value).doubleValue();
            if (DateUtil.isValidExcelDate(serial)) {
                return DateUtil.getLocalDateTime(serial);
            }
        } catch (NumberFormatException ignored) {
            // 统一抛出格式错误
        }
        throw new IllegalArgumentException(
                columnName + "格式错误，建议使用 yyyy-MM-dd HH:mm:ss");
    }
}
