package com.example.videogen.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

/**
 * Reads one or more uploaded .txt files and combines their content into a
 * single source text, so a long document — or a "folder" of text files
 * (chapters/sections uploaded together) — can be planned into scenes the
 * same way a single inline prompt string is.
 */
public final class TextFileReader {

    private TextFileReader() {
    }

    public static String readAndCombine(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one file must be uploaded");
        }

        return files.stream()
                .filter(file -> !file.isEmpty())
                // Deterministic order (e.g. "01-intro.txt", "02-body.txt", ...) rather than
                // whatever order the multipart request happened to list them in.
                .sorted(Comparator.comparing(MultipartFile::getOriginalFilename,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(TextFileReader::readOne)
                .filter(text -> !text.isBlank())
                .reduce((a, b) -> a + "\n\n" + b)
                .orElseThrow(() -> new IllegalArgumentException("Uploaded file(s) contained no readable text"));
    }

    private static String readOne(MultipartFile file) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8).trim();
            String name = file.getOriginalFilename();
            if (name == null || name.isBlank() || content.isEmpty()) {
                return content;
            }
            return "## " + name + "\n" + content;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file " + file.getOriginalFilename(), e);
        }
    }
}
