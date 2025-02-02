package io.yapix.parse.util.doc

import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.javadoc.PsiDocToken
import com.intellij.psi.javadoc.PsiInlineDocTag
import io.yapix.parse.constant.DocumentTags
import net.jkcode.jkutil.common.substringBetween
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import java.util.*

/**
 * PsiDocComment相关工具类
 */
object KtPsiDocCommentHelper: IPsiDocCommentHelper {

    /**
     * 获取@param标记的字段名+字段描述
     * @param element 文档元素，一般指方法
     * @return Map<字段名, 字段描述>
     */
    override fun getParamTagTextMap(element: PsiDocCommentOwner): Map<String, String> {
        val map: MutableMap<String, String> = HashMap()
        val tags = findTagsByName(element, DocumentTags.Param)
        for (tag in tags) {
            val name = tag.getSubjectName() // 参数名
            val description = tag.getContent() // 参数描述
            if(name != null)
                map[name] = description
        }
        return map
    }

    /**
     * 获取标记自定义字段名(不包括字段描述)
     * @param tag 实例变量类型为接口类/实体类  可通过@see 来获取扁平后的字段
     *
     * / **
     * * @see SubInterfaceImpl {field1, field2}
     * * @see SubxInterfaceImpl {field3, field4, field5}
     * *\/
     * interface BaseInterface {}
     *
     * BaseInterface {field1, field2, field3, ...}
     * 至于如何区分field1,field2,field3隶属于哪个实现类, 是用户写desc需要考量的, 而非插件程序逻辑.
     */
    override fun getTagTextSet(element: PsiDocCommentOwner, tag: String): Set<String> {
        return findTagsByName(element, tag)
            .mapNotNull{ obj ->
                obj.getSubjectName()
            }
            .toSet()
    }

    /**
     * 获取标记文本值
     */
    override fun getTagText(element: PsiDocCommentOwner, tagName: String): String? {
        val tag = findTagByName(element, tagName)
        if (tag == null)
            return null

        return tag.getContent()
    }

    /**
     * 获取文档标记内容
     */
    override fun getDocCommentTagText(element: PsiDocCommentOwner, tagName: String): String? {
        val comment = element.findKDoc()
        val tag = comment?.findTagByName(tagName)
        if (tag != null && tag.getSubjectName() != null) {
            return tag.getSubjectName() + tag.getContent()
        }

        return null
    }

    /**
     * 获取文档标题行: 文档第一行
     */
    override fun getDocCommentTitle(element: PsiDocCommentOwner): String? {
        val comment = element.findKDoc() ?: return null
        val title =  comment.allChildren.firstOrNull { o: PsiElement ->
            o is LeafPsiElement && o.elementType.toString() == "KDOC_TEXT"
        }
        return title?.text?.trim()
    }

    /**
     * 获取文档内容
     */
    override fun getDocCommentText(element: PsiDocCommentOwner): String?{
        return element.findKDoc()?.getContent()
    }

    /**
     * 检查是否存在文档注释上的标记
     */
    override fun hasTagByName(element: PsiDocCommentOwner, tagName: String): Boolean{
        return findTagByName(element, tagName) != null
    }

    /**
     * 获取文档注释上的标记
     */
    fun findTagByName(element: PsiDocCommentOwner, tagName: String): KDocTag? {
        val comment = element.findKDoc()
        return comment?.findTagByName(tagName)
    }

    /**
     * 获取文档注释上的标记
     */
    fun findTagsByName(element: PsiDocCommentOwner, tagName: String): List<KDocTag> {
        val comment = element.findKDoc()
        return comment?.findTagsByName(tagName) ?: emptyList()
    }

    /**
     * 获取注释中link标记的内容
     *   对类的引用: 如 java {@link io.yapix.model.Property}, kotlin [io.yapix.model.Property]
     *   返回 io.yapix.model.Property
     */
    override fun getLinkText(element: PsiDocCommentOwner, comment: String): String? {
        if(comment.contains("["))
            return comment.substringBetween("[", "]").trim() // 去空格，否则类路径不对

        return null
    }
}
