package com.talent.agent.service;

import com.talent.agent.exception.AgentBusinessException;
import java.io.ByteArrayInputStream;
import java.util.Locale;
import java.util.Set;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.xml.sax.SAXException;

@Service
public class ResumeTextExtractService {

    private static final Set<String> SUPPORTED_EXT = Set.of("pdf", "doc", "docx");

    private static final Set<String> IMAGE_MEDIA_PREFIXES = Set.of("image/");

    private static final Set<String> ARCHIVE_MEDIA_TYPES = Set.of(
            "application/zip",
            "application/x-zip-compressed",
            "application/x-rar-compressed",
            "application/vnd.rar",
            "application/x-7z-compressed",
            "application/gzip",
            "application/x-tar");

    private final Tika tika = new Tika();
    private final AutoDetectParser parser = new AutoDetectParser();

    public ExtractResult extract(byte[] fileBytes, String fileName, String fileType) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new AgentBusinessException("文件内容为空");
        }
        String ext = normalizeExt(fileType, fileName);
        if (!SUPPORTED_EXT.contains(ext)) {
            throw new AgentBusinessException("不支持的文件格式: " + (ext.isEmpty() ? "未知" : ext));
        }

        String detectedMime;
        try {
            detectedMime = tika.detect(fileBytes, fileName);
        } catch (Exception e) {
            throw new AgentBusinessException("无法识别文件类型: " + e.getMessage(), e);
        }

        rejectUnsupportedMime(detectedMime);

        try {
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            if (StringUtils.hasText(fileName)) {
                metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
            }
            parser.parse(new ByteArrayInputStream(fileBytes), handler, metadata, new ParseContext());
            String text = handler.toString();
            if (!StringUtils.hasText(text)) {
                throw new AgentBusinessException("未能从文件中抽取到有效文本");
            }
            String normalized = text.replaceAll("\\s+", " ").trim();
            if (normalized.isEmpty()) {
                throw new AgentBusinessException("未能从文件中抽取到有效文本");
            }
            return new ExtractResult(normalized, normalized.length(), detectedMime, ext);
        } catch (AgentBusinessException e) {
            throw e;
        } catch (SAXException e) {
            throw new AgentBusinessException("文档解析失败: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new AgentBusinessException("文档解析失败: " + e.getMessage(), e);
        }
    }

    private void rejectUnsupportedMime(String mime) {
        if (!StringUtils.hasText(mime)) {
            return;
        }
        String lower = mime.toLowerCase(Locale.ROOT);
        for (String prefix : IMAGE_MEDIA_PREFIXES) {
            if (lower.startsWith(prefix)) {
                throw new AgentBusinessException("不支持图片格式文件");
            }
        }
        if (ARCHIVE_MEDIA_TYPES.contains(lower)) {
            throw new AgentBusinessException("不支持压缩包格式文件");
        }
        if (!isSupportedDocumentMime(lower)) {
            throw new AgentBusinessException("不支持的文件格式: " + mime);
        }
    }

    private boolean isSupportedDocumentMime(String mime) {
        return mime.contains("pdf")
                || mime.contains("msword")
                || mime.contains("wordprocessingml")
                || "application/octet-stream".equals(mime);
    }

    private String normalizeExt(String fileType, String fileName) {
        if (StringUtils.hasText(fileType)) {
            return fileType.trim().toLowerCase(Locale.ROOT).replace(".", "");
        }
        if (!StringUtils.hasText(fileName)) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public record ExtractResult(String text, int length, String detectedMime, String fileType) {
    }
}
