package com.acme.questionnaire.excel;

import org.apache.poi.ss.usermodel.DateUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * Excel 单元格值解析工具。
 *
 * <p>EasyExcel 在当前读取模式下把行数据交给监听器时使用字符串 Map，所有必填、长度、分值和日期
 * 格式规则都在这里收口，监听器只负责把异常绑定回具体业务列。工具方法不会访问数据库，也不会判断
 * 产品和特性的业务有效性。</p>
 */
public final class ExcelCellParser {
    /**
     * 允许的日期时间格式。
     *
     * <p>包含横线、斜线和 ISO 本地时间，兼容用户手填和部分系统导出的常见格式。</p>
     */
    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    );
    /**
     * 允许的纯日期格式。
     *
     * <p>纯日期导入时按当天 00:00:00 处理，避免用户只填日期时被误判为格式错误。</p>
     */
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.BASIC_ISO_DATE
    );

    private ExcelCellParser() {
    }

    /**
     * 获取并裁剪单元格文本。
     *
     * <p>空单元格返回 null；调用方根据列规则决定 null 是允许留空还是必填错误。</p>
     */
    public static String cell(Map<Integer, String> row, int index) {
        String value = row.get(index);
        return value == null ? null : value.trim();
    }

    /**
     * 解析必填文本列。
     *
     * <p>先复用 nullableText 执行空白归一化和长度限制，再把 null 转成必填错误。</p>
     */
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

    /**
     * 解析可空文本列。
     *
     * <p>返回值已经 trim；全空白按 null 处理，避免把不可见空格写入数据库。</p>
     */
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

    /**
     * 解析必填评分列。
     *
     * <p>评分规则统一为 1-10 的整数，推荐意愿和动态特性评分使用同一套规则。</p>
     */
    public static Integer requiredScore(Map<Integer, String> row,
                                        int index,
                                        String columnName) {
        Integer value = nullableScore(row, index, columnName);
        if (value == null) {
            throw new IllegalArgumentException(columnName + "不能为空");
        }
        return value;
    }

    /**
     * 解析可空评分列。
     *
     * <p>Excel 可能把数字读成 1.0 这样的文本，因此先用 BigDecimal 去掉末尾 0，再要求最终没有小数位。
     * 这样既接受用户常见输入，又拒绝 8.5 这类非整数评分。</p>
     */
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

    /**
     * 解析必填答卷时间。
     *
     * <p>优先尝试文本日期时间和纯日期；最后兼容 Excel 原生日期序号。返回值统一为 LocalDateTime，
     * 供写库层直接写入 pq_answer.answer_time。</p>
     */
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
