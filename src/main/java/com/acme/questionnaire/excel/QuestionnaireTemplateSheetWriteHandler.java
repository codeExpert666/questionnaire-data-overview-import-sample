package com.acme.questionnaire.excel;

import com.alibaba.excel.constant.OrderConstant;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.handler.SheetWriteHandler;
import com.alibaba.excel.write.handler.context.CellWriteHandlerContext;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteWorkbookHolder;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;

/**
 * 问卷导入模板数据页的样式和客户端校验配置。
 *
 * <p>EasyExcel 负责写出工作簿和表头，Apache POI 回调负责补充 Excel 原生能力：
 * 冻结首行、打开筛选、设置列宽、指定文本/日期格式，以及给可枚举字段和评分字段增加数据验证。
 * 这些配置只改善填表体验，导入时仍会在服务端重新执行完整校验，不能把客户端校验视为安全边界。</p>
 *
 * <p>该处理器只注册到“问卷观点导入”数据页，不应用到说明页或字典页。动态评分列范围由
 * QuestionnaireExcelHeaders.fixedHeaderCount() 到 lastColumnIndex 决定，必须与下载时生成的表头保持一致。</p>
 */
public class QuestionnaireTemplateSheetWriteHandler implements SheetWriteHandler, CellWriteHandler {
    /** 第 0 行是表头，真实可填写数据从第 1 行开始。 */
    private static final int FIRST_DATA_ROW_INDEX = 1;

    /** 数据页最后一列的 0-based 索引，包含固定列和所有动态评分列。 */
    private final int lastColumnIndex;
    /** 数据验证覆盖的最后一行索引；配置值 maxDataRows 表示允许填写的数据行数。 */
    private final int lastDataRowIndex;
    /** 表头颜色样式按工作簿复用，避免为每个动态评分列重复创建 CellStyle。 */
    private CellStyle highlightedHeaderStyle;
    private CellStyle defaultHeaderStyle;

    /**
     * 创建数据页写处理器。
     *
     * @param lastColumnIndex 数据页最后一列的 0-based 索引
     * @param maxDataRows 模板允许填写的最大数据行数，不包含表头行
     */
    public QuestionnaireTemplateSheetWriteHandler(int lastColumnIndex, int maxDataRows) {
        this.lastColumnIndex = lastColumnIndex;
        this.lastDataRowIndex = maxDataRows;
    }

    @Override
    public int order() {
        return OrderConstant.FILL_STYLE + 1;
    }

    /**
     * 工作表创建后补充样式和数据验证。
     *
     * <p>表头已经由 EasyExcel 写入，本回调只修改 sheet 级配置，不写业务数据。</p>
     */
    @Override
    public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder,
                                 WriteSheetHolder writeSheetHolder) {
        Sheet sheet = writeSheetHolder.getSheet();
        Workbook workbook = writeWorkbookHolder.getWorkbook();

        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, lastColumnIndex));
        configureColumnWidths(sheet);
        configureDefaultColumnStyles(sheet, workbook);
        configureValidations(sheet);
    }

    @Override
    public void afterCellDispose(CellWriteHandlerContext context) {
        if (!Boolean.TRUE.equals(context.getHead())
                || context.getCell() == null
                || context.getColumnIndex() == null
                || context.getColumnIndex() > lastColumnIndex) {
            return;
        }
        applyHeaderFillStyle(
                context.getCell(),
                isHighlightedHeaderColumn(context.getColumnIndex()));
    }

    /**
     * 设置固定列和动态评分列的显示宽度。
     *
     * <p>宽度只影响模板可读性，不参与导入校验。动态列统一使用 22 个字符宽度，便于展示
     * “特性名称体验”格式。</p>
     */
    private void configureColumnWidths(Sheet sheet) {
        setWidth(sheet, QuestionnaireExcelHeaders.QUESTIONNAIRE_ID, 22);
        setWidth(sheet, QuestionnaireExcelHeaders.PRODUCT_MODEL, 18);
        setWidth(sheet, QuestionnaireExcelHeaders.PRODUCT_CODE, 18);
        setWidth(sheet, QuestionnaireExcelHeaders.ANSWER_TIME, 20);
        setWidth(sheet, QuestionnaireExcelHeaders.ROM_VERSION, 16);
        setWidth(sheet, QuestionnaireExcelHeaders.APP_VERSION, 16);
        setWidth(sheet, QuestionnaireExcelHeaders.FEEDBACK_TEXT, 40);
        setWidth(sheet, QuestionnaireExcelHeaders.SCORE_REASON, 40);
        setWidth(sheet, QuestionnaireExcelHeaders.RECOMMEND_SCORE, 12);
        setWidth(sheet, QuestionnaireExcelHeaders.USER_CATEGORY, 12);
        setWidth(sheet, QuestionnaireExcelHeaders.SENTIMENT, 12);
        setWidth(sheet, QuestionnaireExcelHeaders.OPINION_FEATURE_NAME, 18);
        setWidth(sheet, QuestionnaireExcelHeaders.FEEDBACK_CONTENT_1, 36);
        setWidth(sheet, QuestionnaireExcelHeaders.FEEDBACK_CONTENT_2, 36);
        for (int columnIndex = QuestionnaireExcelHeaders.fixedHeaderCount();
             columnIndex <= lastColumnIndex;
             columnIndex++) {
            setWidth(sheet, columnIndex, 22);
        }
    }

    /**
     * 设置关键列的默认单元格格式。
     *
     * <p>问卷 ID、产品编码、版本号和特性分类名称使用文本格式，避免 Excel 自动把编码、名称或版本号
     * 转成数字、科学计数法或日期。答卷时间使用日期时间格式，便于用户录入和检查。</p>
     */
    private void configureDefaultColumnStyles(Sheet sheet, Workbook workbook) {
        DataFormat dataFormat = workbook.createDataFormat();

        CellStyle textStyle = workbook.createCellStyle();
        textStyle.setDataFormat(dataFormat.getFormat("@"));
        sheet.setDefaultColumnStyle(QuestionnaireExcelHeaders.QUESTIONNAIRE_ID, textStyle);
        sheet.setDefaultColumnStyle(QuestionnaireExcelHeaders.PRODUCT_CODE, textStyle);
        sheet.setDefaultColumnStyle(QuestionnaireExcelHeaders.ROM_VERSION, textStyle);
        sheet.setDefaultColumnStyle(QuestionnaireExcelHeaders.APP_VERSION, textStyle);
        sheet.setDefaultColumnStyle(QuestionnaireExcelHeaders.OPINION_FEATURE_NAME, textStyle);

        CellStyle dateTimeStyle = workbook.createCellStyle();
        dateTimeStyle.setDataFormat(dataFormat.getFormat("yyyy-mm-dd hh:mm:ss"));
        sheet.setDefaultColumnStyle(QuestionnaireExcelHeaders.ANSWER_TIME, dateTimeStyle);
    }

    /**
     * 配置模板可在 Excel 客户端提前拦截的字段约束。
     *
     * <p>用户归类和情感观点是固定枚举；推荐意愿和所有动态评分列都是 1-10 的整数。
     * 产品适用特性、问卷多观点连续性、字典编码存在性等跨字段规则无法通过 Excel 原生验证表达，
     * 必须在导入监听器中执行。</p>
     */
    private void configureValidations(Sheet sheet) {
        addExplicitListValidation(sheet,
                QuestionnaireExcelHeaders.USER_CATEGORY,
                new String[]{"推荐者", "中立者", "贬损者", "未知"});
        addExplicitListValidation(sheet,
                QuestionnaireExcelHeaders.SENTIMENT,
                new String[]{"好评", "中评", "差评", "未反馈"});
        addIntegerScoreValidation(sheet, QuestionnaireExcelHeaders.RECOMMEND_SCORE);
        for (int columnIndex = QuestionnaireExcelHeaders.fixedHeaderCount();
             columnIndex <= lastColumnIndex;
             columnIndex++) {
            addIntegerScoreValidation(sheet, columnIndex);
        }
    }

    /**
     * 为枚举列增加下拉列表校验。
     *
     * <p>数据验证覆盖所有允许填写的数据行；错误样式为 STOP，提示用户只能选择预设值。</p>
     */
    private void addExplicitListValidation(Sheet sheet, int columnIndex, String[] values) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createExplicitListConstraint(values);
        CellRangeAddressList regions = new CellRangeAddressList(
                FIRST_DATA_ROW_INDEX, lastDataRowIndex, columnIndex, columnIndex);
        DataValidation validation = helper.createValidation(constraint, regions);
        validation.setShowErrorBox(true);
        validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        validation.createErrorBox("输入错误", "请选择下拉列表中的值");
        sheet.addValidationData(validation);
    }

    private void applyHeaderFillStyle(Cell cell, boolean highlighted) {
        CellStyle style = highlighted ? highlightedHeaderStyle : defaultHeaderStyle;
        if (style == null) {
            style = cell.getSheet().getWorkbook().createCellStyle();
            style.cloneStyleFrom(cell.getCellStyle());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setFillForegroundColor(
                    highlighted
                            ? IndexedColors.YELLOW.getIndex()
                            : IndexedColors.GREY_25_PERCENT.getIndex());
            if (highlighted) {
                highlightedHeaderStyle = style;
            } else {
                defaultHeaderStyle = style;
            }
        }
        cell.setCellStyle(style);
    }

    private boolean isHighlightedHeaderColumn(int columnIndex) {
        return columnIndex == QuestionnaireExcelHeaders.USER_CATEGORY
                || columnIndex == QuestionnaireExcelHeaders.SENTIMENT
                || columnIndex == QuestionnaireExcelHeaders.OPINION_FEATURE_NAME
                || columnIndex == QuestionnaireExcelHeaders.FEEDBACK_CONTENT_1
                || columnIndex == QuestionnaireExcelHeaders.FEEDBACK_CONTENT_2;
    }

    /**
     * 为评分列增加 1-10 整数校验。
     *
     * <p>允许空单元格，因为动态特性评分在产品不适用时必须留空；推荐意愿列虽然业务上必填，
     * 也会在服务端导入校验中再次检查，避免用户绕过 Excel 客户端限制。</p>
     */
    private void addIntegerScoreValidation(Sheet sheet, int columnIndex) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createIntegerConstraint(
                DataValidationConstraint.OperatorType.BETWEEN, "1", "10");
        CellRangeAddressList regions = new CellRangeAddressList(
                FIRST_DATA_ROW_INDEX, lastDataRowIndex, columnIndex, columnIndex);
        DataValidation validation = helper.createValidation(constraint, regions);
        validation.setEmptyCellAllowed(true);
        validation.setShowErrorBox(true);
        validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        validation.createErrorBox("输入错误", "评分必须是1-10的整数");
        sheet.addValidationData(validation);
    }

    /**
     * 按字符宽度设置 Excel 列宽。
     *
     * <p>Excel 单列最大宽度为 255 个字符，这里统一裁剪，避免后续调整列宽时写出非法值。</p>
     */
    private void setWidth(Sheet sheet, int columnIndex, int characters) {
        sheet.setColumnWidth(columnIndex, Math.min(characters, 255) * 256);
    }
}
