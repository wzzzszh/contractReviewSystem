package com.szh.contractReviewSystem.agent.docx;

import dev.langchain4j.agent.tool.Tool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class DocxFileTools {

    @Tool("Read a UTF-8 text file from an absolute path. Use this for unpacked XML, RELS, TXT, or MD files.")
    public String readTextFile(String absolutePath) throws IOException {
        Path path = requireAbsolutePath(absolutePath);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    @Tool("Write UTF-8 text content to an absolute path. This overwrites the existing file and creates parent directories if needed.")
    public String writeTextFile(String absolutePath, String content) throws IOException {
        Path path = requireAbsolutePath(absolutePath);
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, content == null ? "" : content, StandardCharsets.UTF_8);
        return "Wrote file: " + path.toAbsolutePath().normalize();
    }

    @Tool("Check whether a file or directory exists at an absolute path.")
    public boolean pathExists(String absolutePath) {
        Path path = requireAbsolutePath(absolutePath);
        return Files.exists(path);
    }

    private Path requireAbsolutePath(String absolutePath) {
        if (absolutePath == null || absolutePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Path must not be blank");
        }
        Path path = Path.of(absolutePath.trim()).toAbsolutePath().normalize();
        if (!path.isAbsolute()) {
            throw new IllegalArgumentException("Path must be absolute: " + absolutePath);
        }
        return path;
    }
}
