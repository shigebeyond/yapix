package io.yapix.parse.parser;

import com.google.common.collect.Sets;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import io.yapix.config.YapixConfig;
import io.yapix.model.DataTypes;
import io.yapix.model.Property;
import io.yapix.parse.constant.SpringConstants;
import io.yapix.parse.util.PsiAnnotationUtils;
import java.util.Set;

/**
 * 时间相关类型解析
 */
public class DateParser {

    private final YapixConfig settings;

    public DateParser(YapixConfig settings) {
        this.settings = settings;
    }

    /**
     * 处理请求参数为时间的场景
     */
    public boolean handle(Property property, PsiParameter parameter) {
        if (!isDateType(parameter.getType())) {
            return false;
        }
        // 1: @DateTimeFormat优先
        String dateFormat = PsiAnnotationUtils
                .getStringAttributeValue(parameter, SpringConstants.DateTimeFormat, "pattern");
        if (dateFormat != null) {
            property.setType(DataTypes.STRING);
            property.setDateFormat(dateFormat);
            return true;
        }

        // 2: 全局配置
        String dateTimeFormatMvc = settings.getDateTimeFormatMvc();
        if (DataTypes.INTEGER.equals(dateTimeFormatMvc)) {
            property.setType(DataTypes.INTEGER);
            property.setDateFormat(null);
        } else {
            property.setType(DataTypes.STRING);
            property.setDateFormat(dateTimeFormatMvc);
        }
        return true;
    }

    /**
     * 字段处理
     */
    public boolean handle(Property property, PsiField field) {
        if (!isDateType(field.getType())) {
            return false;
        }
        // 1: @DateTimeFormat优先
        String dateFormat = PsiAnnotationUtils
                .getStringAttributeValue(field, SpringConstants.DateTimeFormat, "pattern");
        if (dateFormat != null) {
            property.setType(DataTypes.STRING);
            property.setDateFormat(dateFormat);
            return true;
        }

        // 2: @JsonFormat优先
        String jsonFormat = PsiAnnotationUtils
                .getStringAttributeValue(field, SpringConstants.JsonFormat, "pattern");
        if (jsonFormat != null) {
            property.setType(DataTypes.STRING);
            property.setDateFormat(jsonFormat);
            return true;
        }

        // 3: 全局配置
        String dateTimeFormatMvc = settings.getDateTimeFormatJson();
        if (DataTypes.INTEGER.equals(dateTimeFormatMvc)) {
            property.setType(DataTypes.INTEGER);
            property.setDateFormat(null);
        } else {
            property.setType(DataTypes.STRING);
            property.setDateFormat(dateTimeFormatMvc);
        }
        return true;
    }

    public static boolean isDateType(PsiType type) {
        Set<String> dateTypes = Sets.newHashSet(
                "java.util.Date",
                "java.sql.Date", "java.sql.Timestamp",
                "java.time.LocalDate", "java.time.LocalDateTime", "java.time.LocalTime"
        );
        return dateTypes.contains(type.getCanonicalText());
    }

}
