package com.acme.questionnaire.excel;

import com.alibaba.excel.write.handler.SheetWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteWorkbookHolder;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;

public class QuestionnaireTemplateSheetWriteHandler implements SheetWriteHandler {
    private static final int FIRST_DATA_ROW_INDEX = 1;

    private final int lastColumnIndex;
    private final int lastDataRowIndex;

    public QuestionnaireTemplateSheetWriteHandler(int lastColumnIndex, int maxDataRows) {
        this.lastColumnIndex = lastColumnIndex;
        this.lastDataRowIndex = maxDataRows;
    }

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
        setWidth(sheet, QuestionnaireExcelHeaders.OPINION_FEATURE_CODE, 18);
        setWidth(sheet, QuestionnaireExcelHeaders.FEEDBACK_CONTENT_1, 36);
        setWidth(sheet, QuestionnaireExcelHeaders.FEEDBACK_CONTENT_2, 36);
        for (int columnIndex = QuestionnaireExcelHeaders.fixedHeaderCount();
             columnIndex <= lastColumnIndex;
             columnIndex++) {
            setWidth(sheet, columnIndex, 22);
        }
    }

    private void configureDefaultColumnStyles(Sheet sheet, Workbook workbook) {
        DataFormat dataFormat = workbook.createDataFormat();

        CellStyle textStyle = workbook.createCellStyle();
        textStyle.setDataFormat(dataFormat.getFormat("@"));
        sheet.setDefaultColumnStyle(QuestionnaireExcelHeaders.QUESTIONNAIRE_ID, textStyle);
        sheet.setDefaultColumnStyle(QuestionnaireExcelHeaders.PRODUCT_CODE, textStyle);
        sheet.setDefaultColumnStyle(QuestionnaireExcelHeaders.ROM_VERSION, textStyle);
        sheet.setDefaultColumnStyle(QuestionnaireExcelHeaders.APP_VERSION, textStyle);
        sheet.setDefaultColumnStyle(QuestionnaireExcelHeaders.OPINION_FEATURE_CODE, textStyle);

        CellStyle dateTimeStyle = workbook.createCellStyle();
        dateTimeStyle.setDataFormat(dataFormat.getFormat("yyyy-mm-dd hh:mm:ss"));
        sheet.setDefaultColumnStyle(QuestionnaireExcelHeaders.ANSWER_TIME, dateTimeStyle);
    }

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

    private void setWidth(Sheet sheet, int columnIndex, int characters) {
        sheet.setColumnWidth(columnIndex, Math.min(characters, 255) * 256);
    }
}
