package io.yapix.parse.parser;

import static io.yapix.parse.util.PsiGenericUtils.splitTypeAndGenericPair;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTypesUtil;
import io.yapix.config.YapixConfig;
import io.yapix.model.Property;
import io.yapix.parse.util.PsiTypeUtils;
import io.yapix.parse.util.PsiUtils;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

/**
 * 方法返回值解析
 *
 * @see #parse(PsiMethod)
 */
public class ResponseParser {

    private final Project project;
    private final Module module;
    private final YapixConfig settings;
    private final KernelParser kernelParser;
    private final ParseHelper parseHelper;

    public ResponseParser(Project project, Module module, YapixConfig settings) {
        this.project = project;
        this.module = module;
        this.settings = settings;
        this.kernelParser = new KernelParser(project, module, settings, true);
        this.parseHelper = new ParseHelper(project, module);
    }

    public Property parse(PsiMethod method) {
        PsiType returnType = method.getReturnType();
        if (returnType == null) {
            return null;
        }
        PsiType type = returnType;
        String typeText = returnType.getCanonicalText();

        String unwrappedType = getUnwrapType(returnType);
        if (unwrappedType != null) {
            String[] types = splitTypeAndGenericPair(unwrappedType);
            PsiClass psiClass = PsiUtils.findPsiClass(this.project, this.module, types[0]);
            type = PsiTypesUtil.getClassType(psiClass);
            typeText = unwrappedType;
        } else {
            // 包装类处理
            PsiClass returnClass = getWrapperPsiClass(method);
            if (returnClass != null) {
                type = PsiTypesUtil.getClassType(returnClass);
                typeText = type.getCanonicalText() + "<" + returnType.getCanonicalText() + ">";
            }
        }

        // 解析
        Property item = kernelParser.parseType(type, typeText);
        if (item != null) {
            item.setDescription(parseHelper.getTypeDescription(type));
        }
        return item;
    }

    private String getUnwrapType(PsiType type) {
        List<String> unwrapTypes = settings.getReturnUnwrapTypes();
        String[] types = splitTypeAndGenericPair(type.getCanonicalText());
        Optional<String> unwrapOpt = unwrapTypes.stream().filter(t -> t.equals(types[0])).findAny();
        if (unwrapOpt.isPresent()) {
            return types[1];
        }
        return null;
    }

    /**
     * 返回需要需要的包装类
     */
    private PsiClass getWrapperPsiClass(PsiMethod method) {
        if (StringUtils.isEmpty(settings.getReturnWrapType())) {
            return null;
        }
        PsiClass returnClass = PsiUtils.findPsiClass(this.project, this.module, settings.getReturnWrapType());
        if (returnClass == null) {
            return null;
        }

        // 是否是byte[]
        PsiType returnType = method.getReturnType();
        if (PsiTypeUtils.isBytes(returnType)) {
            return null;
        }

        // 是否是相同类型
        String[] types = splitTypeAndGenericPair(returnType.getCanonicalText());
        String theReturnType = types[0];
        if (Objects.equals(theReturnType, returnClass.getQualifiedName())) {
            return null;
        }
        return returnClass;
    }

}
