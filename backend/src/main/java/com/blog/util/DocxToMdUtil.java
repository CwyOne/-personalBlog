package com.blog.util;

import com.blog.service.OssService;
import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

/**
 * DOCX → Markdown 转换工具（增强版 + 详细日志）
 * <p>
 * 基于 Apache POI 解析 .docx 文档，输出标准 Markdown 文本。
 * 支持：标题（三级识别策略）、段落、列表、表格、加粗/斜体、超链接、代码块、图片。
 * </p>
 *
 * <h3>标题识别优先级（按顺序执行）</h3>
 * <ol>
 *   <li><b>内置标题样式</b>：通过样式 ID 和样式名双重匹配 Heading1~6 / 标题1~6</li>
 *   <li><b>大纲级别</b>：段落 XML 属性 outlineLvl 为 0~5</li>
 *   <li><b>字号+加粗+居中</b>（兜底）：字号 ≥ 16pt 且全部加粗且居中对齐 → 映射为 ##</li>
 * </ol>
 */
public class DocxToMdUtil {

    private static final Logger log = LoggerFactory.getLogger(DocxToMdUtil.class);

    /** 正文字号基准（半磅），16pt = 32 半磅 */
    private static final int HEADING_MIN_FONT_SIZE_HALF_PT = 32;

    /** 兜底标题映射的 Markdown 级别 */
    private static final int FALLBACK_HEADING_LEVEL = 2;

    /** 等宽字体集合 */
    private static final Set<String> MONOSPACE_FONTS = Set.of(
            "consolas", "courier", "courier new", "monaco", "menlo",
            "lucida console", "dejavu sans mono", "source code pro",
            "fira code", "jetbrains mono", "inconsolata", "roboto mono",
            "monospace", "fixedsys", "terminal", "console"
    );

    /** 编程语言关键词 */
    private static final Map<String, String> LANG_KEYWORDS = new LinkedHashMap<>();

    static {
        LANG_KEYWORDS.put("public class|private class|protected class|import java\\.|System\\.out|public static void main", "java");
        LANG_KEYWORDS.put("@RequestMapping|@GetMapping|@PostMapping|@RestController|@Service|@Autowired", "java");
        LANG_KEYWORDS.put("def |import |from |print\\(|class .*:\\s*$|if __name__", "python");
        LANG_KEYWORDS.put("function |const |let |var |=>|console\\.log|import .* from|export ", "javascript");
        LANG_KEYWORDS.put("<\\?php|echo |\\$[a-zA-Z]|function .*\\(", "php");
        LANG_KEYWORDS.put("#include|int main|printf|scanf|std::", "cpp");
        LANG_KEYWORDS.put("func |package |import |fmt\\.Print|go func|defer ", "go");
        LANG_KEYWORDS.put("fn |let mut|println!|use |pub struct|impl ", "rust");
        LANG_KEYWORDS.put("SELECT |INSERT |UPDATE |DELETE |CREATE TABLE|ALTER TABLE|DROP TABLE", "sql");
        LANG_KEYWORDS.put("apiVersion:|kind:|metadata:|spec:|containers:", "yaml");
        LANG_KEYWORDS.put("spring\\.|server\\.port|datasource|mybatis", "yaml");
        LANG_KEYWORDS.put("\\{|\"[a-zA-Z]+\":|\"version\"|\"dependencies\"", "json");
        LANG_KEYWORDS.put("<html|<div|<span|<script|<style|<!DOCTYPE", "html");
        LANG_KEYWORDS.put("FROM |RUN |COPY |EXPOSE |CMD |ENTRYPOINT|WORKDIR ", "dockerfile");
        LANG_KEYWORDS.put("server \\{|location |listen |proxy_pass|upstream ", "nginx");
    }

    /** 标题级别匹配模式（用于样式 ID 和样式名） */
    private static final Pattern HEADING_PATTERN = Pattern.compile(
            "(?i)(?:heading|标题)[\\s\\-_]?(\\d)"
    );

    /**
     * 将 DOCX 文件转换为 Markdown
     */
    public static String convert(InputStream inputStream, OssService ossService) throws Exception {
        log.info("========== 开始解析 DOCX 文档 ==========");

        XWPFDocument document = new XWPFDocument(inputStream);
        StringBuilder md = new StringBuilder();
        int paraIndex = 0;

        for (IBodyElement element : document.getBodyElements()) {
            if (element instanceof XWPFParagraph) {
                XWPFParagraph paragraph = (XWPFParagraph) element;
                paraIndex++;
                String paraMd = convertParagraph(paragraph, document, ossService, paraIndex);
                if (paraMd != null && !paraMd.isEmpty()) {
                    md.append(paraMd).append("\n\n");
                }
            } else if (element instanceof XWPFTable) {
                XWPFTable table = (XWPFTable) element;
                paraIndex++;
                log.info("[段落 #{}] 类型: 表格 ({}行)", paraIndex, table.getRows().size());
                String tableMd = convertTable(table);
                if (tableMd != null && !tableMd.isEmpty()) {
                    md.append(tableMd).append("\n\n");
                }
            }
        }

        document.close();

        String result = md.toString().replaceAll("\\n{3,}", "\n\n").trim();
        log.info("========== DOCX 解析完成，共 {} 字符 ==========", result.length());
        return result;
    }

    // ─────────────────────────────────────────────
    // 标题识别（三级优先级 + 详细日志）
    // ─────────────────────────────────────────────

    /**
     * 判断段落是否为标题，返回标题级别（1~6），非标题返回 -1。
     */
    private static int detectHeadingLevel(XWPFParagraph paragraph, XWPFDocument document, int paraIndex) {
        // ── 优先级 1：内置标题样式（双重匹配：样式 ID + 样式名）──
        String styleId = paragraph.getStyle();
        String styleName = getStyleName(document, styleId);

        log.debug("[段落 #{}] 样式ID=\"{}\" 样式名=\"{}\"", paraIndex, styleId, styleName);

        // 先匹配样式 ID（如 "Heading1", "1", "标题1"）
        int levelFromId = matchHeadingLevel(styleId, paraIndex, "样式ID");
        if (levelFromId > 0) return levelFromId;

        // 再匹配样式名（如 "heading 1", "标题 1", "Heading 1"）
        if (styleName != null && !styleName.equals(styleId)) {
            int levelFromName = matchHeadingLevel(styleName, paraIndex, "样式名");
            if (levelFromName > 0) return levelFromName;
        }

        // 补充：WPS 特殊处理 — 某些 WPS 文件样式 ID 仅为数字 "1"~"6"
        if (styleId != null && styleId.matches("^[1-6]$")) {
            // 需要确认该样式确实是标题样式（通过样式名验证）
            if (styleName != null && (styleName.toLowerCase().contains("heading") || styleName.contains("标题"))) {
                int level = Integer.parseInt(styleId);
                log.info("[段落 #{}] ✓ 优先级1命中(WPS数字ID) - 样式ID:\"{}\" 样式名:\"{}\" → Heading {}",
                        paraIndex, styleId, styleName, level);
                return level;
            }
        }

        // ── 优先级 2：大纲级别 ──
        try {
            if (paragraph.getCTP() != null && paragraph.getCTP().getPPr() != null) {
                var outlineLvl = paragraph.getCTP().getPPr().getOutlineLvl();
                if (outlineLvl != null) {
                    int val = outlineLvl.getVal().intValue();
                    int level = val + 1;
                    if (level >= 1 && level <= 6) {
                        log.info("[段落 #{}] ✓ 优先级2命中 - outlineLvl={} → Heading {}", paraIndex, val, level);
                        return level;
                    }
                } else {
                    log.debug("[段落 #{}] ✗ 优先级2未命中 - outlineLvl 属性为空", paraIndex);
                }
            } else {
                log.debug("[段落 #{}] ✗ 优先级2未命中 - 段落无 PPr 属性", paraIndex);
            }
        } catch (Exception e) {
            log.warn("[段落 #{}] ✗ 优先级2读取 outlineLvl 异常: {}", paraIndex, e.getMessage());
        }

        // ── 优先级 3（兜底）：字号 + 加粗 + 居中 ──
        FallbackHeadingResult result = checkFallbackHeading(paragraph, paraIndex);
        if (result.isHeading) {
            log.info("[段落 #{}] ✓ 优先级3命中 - 字号:{}pt 全部加粗:{} 居中:{} → Heading {}",
                    paraIndex, result.maxFontSizePt, result.allBold, result.centered, FALLBACK_HEADING_LEVEL);
            return FALLBACK_HEADING_LEVEL;
        }

        return -1;
    }

    /**
     * 从文档样式表中查找样式 ID 对应的显示名称
     */
    private static String getStyleName(XWPFDocument document, String styleId) {
        if (styleId == null || document == null) return null;
        try {
            XWPFStyle style = document.getStyles().getStyle(styleId);
            if (style != null) {
                String name = style.getName();
                log.trace("样式查找: ID=\"{}\" → 名称=\"{}\"", styleId, name);
                return name;
            }
        } catch (Exception e) {
            log.debug("样式查找异常: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 从样式名称/ID 中匹配标题级别
     */
    private static int matchHeadingLevel(String value, int paraIndex, String source) {
        if (value == null) return -1;

        java.util.regex.Matcher m = HEADING_PATTERN.matcher(value);
        if (m.find()) {
            int level = Integer.parseInt(m.group(1));
            if (level >= 1 && level <= 6) {
                log.info("[段落 #{}] ✓ 优先级1命中 - {}: \"{}\" → Heading {}", paraIndex, source, value, level);
                return level;
            }
        }
        return -1;
    }

    /** 兜底标题检测结果 */
    private static class FallbackHeadingResult {
        boolean isHeading;
        boolean allBold;
        boolean centered;
        double maxFontSizePt;
    }

    /**
     * 兜底标题判断：字号 ≥ 16pt 且全部加粗且居中对齐
     */
    private static FallbackHeadingResult checkFallbackHeading(XWPFParagraph paragraph, int paraIndex) {
        FallbackHeadingResult result = new FallbackHeadingResult();

        if (paragraph.getNumID() != null) {
            log.debug("[段落 #{}] ✗ 优先级3跳过 - 是列表项", paraIndex);
            return result;
        }

        String style = paragraph.getStyle();
        if (style != null && style.toLowerCase().contains("quote")) {
            log.debug("[段落 #{}] ✗ 优先级3跳过 - 是引用样式", paraIndex);
            return result;
        }

        List<XWPFRun> runs = paragraph.getRuns();
        if (runs == null || runs.isEmpty()) return result;

        boolean hasContent = false;
        boolean allBold = true;
        boolean allLargeFont = true;
        int maxFontSize = 0;

        for (XWPFRun run : runs) {
            String runText = run.text();
            if (runText == null || runText.trim().isEmpty()) continue;
            hasContent = true;

            if (!run.isBold()) allBold = false;

            Double fontSizeObj = run.getFontSizeAsDouble();
            int fontSize = fontSizeObj != null ? fontSizeObj.intValue() : 0;
            if (fontSize > 0) {
                maxFontSize = Math.max(maxFontSize, fontSize);
            } else {
                allLargeFont = false;
            }
        }

        if (!hasContent) return result;

        boolean isCentered = paragraph.getAlignment() != null
                && paragraph.getAlignment() == ParagraphAlignment.CENTER;

        result.allBold = allBold;
        result.centered = isCentered;
        result.maxFontSizePt = maxFontSize / 2.0;

        boolean fontSizeOk = allLargeFont && maxFontSize >= HEADING_MIN_FONT_SIZE_HALF_PT;
        log.debug("[段落 #{}] 优先级3检查 - 字号:{}pt(≥16:{}) 全部加粗:{} 居中:{} → {}",
                paraIndex, maxFontSize / 2.0, fontSizeOk, allBold, isCentered,
                (allBold && fontSizeOk && isCentered) ? "命中" : "未命中");

        result.isHeading = allBold && fontSizeOk && isCentered;
        return result;
    }

    /**
     * 清理标题文本
     */
    private static String cleanHeadingText(String text) {
        if (text == null) return "";
        String cleaned = text.replaceAll("\\s+", " ").trim();
        cleaned = cleaned.replaceAll("^[\\s:：.。,，;；\\-—]+", "");
        cleaned = cleaned.replaceAll("[\\s:：.。,，;；\\-—]+$", "");
        return cleaned;
    }

    // ─────────────────────────────────────────────
    // 段落转换
    // ─────────────────────────────────────────────

    private static String convertParagraph(XWPFParagraph paragraph, XWPFDocument document,
                                            OssService ossService, int paraIndex) throws Exception {
        String text = paragraph.getText();
        String style = paragraph.getStyle();
        String textPreview = text != null ? (text.length() > 40 ? text.substring(0, 40) + "..." : text) : "(空)";

        log.info("[段落 #{}] ──── 开始解析 ────", paraIndex);
        log.info("[段落 #{}] 样式: \"{}\"  文本: \"{}\"", paraIndex, style, textPreview);

        if (text == null || text.trim().isEmpty()) {
            if (!containsImage(paragraph)) {
                log.debug("[段落 #{}] 跳过 - 空段落且无图片", paraIndex);
                return "";
            }
        }

        // 1. 代码块检测
        if (isCodeBlockStyle(paragraph, paraIndex)) {
            log.info("[段落 #{}] → 代码块", paraIndex);
            return convertCodeBlock(paragraph);
        }

        // 2. 图片段落
        String images = extractImages(paragraph, ossService, paraIndex);
        if (images != null && !images.isEmpty()) {
            log.info("[段落 #{}] → 图片", paraIndex);
            return images;
        }

        if (text == null) return "";

        // 3. 标题识别（传入 document 以查找样式名）
        int headingLevel = detectHeadingLevel(paragraph, document, paraIndex);
        if (headingLevel > 0) {
            String headingText = cleanHeadingText(text);
            if (!headingText.isEmpty()) {
                String md = "#".repeat(headingLevel) + " " + headingText;
                log.info("[段落 #{}] → 标题: {}", paraIndex, md);
                return md;
            }
        }

        // 4. 列表项
        if (isListItem(paragraph)) {
            String listItem = convertListItem(paragraph);
            log.info("[段落 #{}] → 列表项", paraIndex);
            return listItem;
        }

        // 5. 普通段落
        String formatted = convertRuns(paragraph);
        if (formatted.trim().isEmpty()) return "";
        log.debug("[段落 #{}] → 普通段落", paraIndex);
        return formatted;
    }

    // ─────────────────────────────────────────────
    // 代码块
    // ─────────────────────────────────────────────

    private static boolean isCodeBlockStyle(XWPFParagraph paragraph, int paraIndex) {
        String style = paragraph.getStyle();
        if (style != null && style.toLowerCase().contains("code")) {
            log.debug("[段落 #{}] 代码块检测: 样式名含 \"code\" → 是代码块", paraIndex);
            return true;
        }

        List<XWPFRun> runs = paragraph.getRuns();
        if (runs == null || runs.isEmpty()) return false;

        boolean allMonospace = true;
        boolean hasContent = false;
        String firstFont = null;
        for (XWPFRun run : runs) {
            String runText = run.text();
            if (runText == null || runText.trim().isEmpty()) continue;
            hasContent = true;
            String fontName = run.getFontName();
            if (firstFont == null) firstFont = fontName;
            if (fontName == null || !MONOSPACE_FONTS.contains(fontName.toLowerCase())) {
                allMonospace = false;
                break;
            }
        }

        if (hasContent && allMonospace) {
            log.debug("[段落 #{}] 代码块检测: 全部等宽字体 \"{}\" → 是代码块", paraIndex, firstFont);
        }
        return hasContent && allMonospace;
    }

    private static String convertCodeBlock(XWPFParagraph paragraph) {
        StringBuilder code = new StringBuilder();
        for (XWPFRun run : paragraph.getRuns()) {
            String runText = run.text();
            if (runText != null) code.append(runText);
        }
        String codeText = code.toString();
        if (codeText.trim().isEmpty()) return "";
        String lang = detectLanguage(codeText);
        return "```" + lang + "\n" + codeText + "\n```";
    }

    private static String detectLanguage(String code) {
        for (Map.Entry<String, String> entry : LANG_KEYWORDS.entrySet()) {
            Pattern pattern = Pattern.compile(entry.getKey(), Pattern.MULTILINE);
            if (pattern.matcher(code).find()) return entry.getValue();
        }
        return "";
    }

    // ─────────────────────────────────────────────
    // 图片
    // ─────────────────────────────────────────────

    private static boolean containsImage(XWPFParagraph paragraph) {
        for (XWPFRun run : paragraph.getRuns()) {
            if (run.getEmbeddedPictures() != null && !run.getEmbeddedPictures().isEmpty()) return true;
        }
        return false;
    }

    private static String extractImages(XWPFParagraph paragraph, OssService ossService, int paraIndex) throws Exception {
        StringBuilder images = new StringBuilder();

        for (XWPFRun run : paragraph.getRuns()) {
            List<XWPFPicture> pictures = run.getEmbeddedPictures();
            if (pictures == null || pictures.isEmpty()) continue;

            for (XWPFPicture picture : pictures) {
                String fileName = picture.getPictureData().getFileName();
                byte[] imageData = picture.getPictureData().getData();

                String url;
                if (ossService != null) {
                    try (InputStream is = new ByteArrayInputStream(imageData)) {
                        url = ossService.upload(is, fileName, "docx-images");
                    }
                    log.info("[段落 #{}] 图片上传成功: {} → {}", paraIndex, fileName, url);
                } else {
                    log.warn("[段落 #{}] 跳过图片上传（OssService 为 null）: {}", paraIndex, fileName);
                    continue;
                }

                String altText = fileName != null ? fileName.replaceFirst("\\.[^.]+$", "") : "图片";
                images.append("![").append(altText).append("](").append(url).append(")\n\n");
            }
        }

        return images.toString().trim();
    }

    // ─────────────────────────────────────────────
    // 列表
    // ─────────────────────────────────────────────

    private static boolean isListItem(XWPFParagraph paragraph) {
        if (paragraph.getNumID() != null) return true;
        String text = paragraph.getText();
        if (text != null) {
            String trimmed = text.trim();
            if (trimmed.matches("^[•·●○◦▪▸►→\\-\\*]\\s.*") ||
                trimmed.matches("^\\d+[.、)]\\s.*")) {
                return true;
            }
        }
        return false;
    }

    private static String convertListItem(XWPFParagraph paragraph) {
        String text = convertRuns(paragraph);
        if (text.trim().isEmpty()) return "";
        String trimmed = text.trim();
        if (trimmed.matches("^\\d+[.、)]\\s.*")) return trimmed;
        return "- " + trimmed;
    }

    // ─────────────────────────────────────────────
    // 表格
    // ─────────────────────────────────────────────

    private static String convertTable(XWPFTable table) {
        List<XWPFTableRow> rows = table.getRows();
        if (rows == null || rows.isEmpty()) return "";

        StringBuilder md = new StringBuilder();
        int colCount = 0;
        for (XWPFTableRow row : rows) {
            colCount = Math.max(colCount, row.getTableCells().size());
        }

        XWPFTableRow headerRow = rows.get(0);
        md.append("|");
        for (int c = 0; c < colCount; c++) {
            md.append(" ").append(getCellText(headerRow, c)).append(" |");
        }
        md.append("\n|");
        for (int c = 0; c < colCount; c++) {
            md.append(" --- |");
        }
        md.append("\n");

        for (int r = 1; r < rows.size(); r++) {
            XWPFTableRow row = rows.get(r);
            md.append("|");
            for (int c = 0; c < colCount; c++) {
                md.append(" ").append(getCellText(row, c)).append(" |");
            }
            md.append("\n");
        }

        return md.toString();
    }

    private static String getCellText(XWPFTableRow row, int colIndex) {
        List<XWPFTableCell> cells = row.getTableCells();
        if (colIndex >= cells.size()) return "";
        XWPFTableCell cell = cells.get(colIndex);
        StringBuilder text = new StringBuilder();
        for (XWPFParagraph p : cell.getParagraphs()) {
            String pText = p.getText();
            if (pText != null && !pText.trim().isEmpty()) {
                if (text.length() > 0) text.append(" ");
                text.append(pText.trim());
            }
        }
        return text.toString().replace("|", "\\|");
    }

    // ─────────────────────────────────────────────
    // Runs 格式化
    // ─────────────────────────────────────────────

    private static String convertRuns(XWPFParagraph paragraph) {
        StringBuilder result = new StringBuilder();

        for (XWPFRun run : paragraph.getRuns()) {
            String text = run.text();
            if (text == null || text.isEmpty()) continue;
            if (!run.getEmbeddedPictures().isEmpty()) continue;

            boolean bold = run.isBold();
            boolean italic = run.isItalic();
            String hyperlink = getHyperlink(run);

            String formatted = text;
            if (hyperlink != null) {
                formatted = "[" + text + "](" + hyperlink + ")";
            } else if (bold && italic) {
                formatted = "***" + text + "***";
            } else if (bold) {
                formatted = "**" + text + "**";
            } else if (italic) {
                formatted = "*" + text + "*";
            }

            result.append(formatted);
        }

        return result.toString();
    }

    private static String getHyperlink(XWPFRun run) {
        String runText = run.text();
        if (runText != null && runText.matches("https?://\\S+")) return runText;
        return null;
    }
}
