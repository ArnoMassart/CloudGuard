package com.cloudmen.cloudguard.utility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityEmailHtmlTest {

    @Test
    void document_includesHeaderTitleAndFooter() {
        String html = SecurityEmailHtml.document("<p>Hi</p>", "Foot");
        assertTrue(html.contains("<!DOCTYPE html>"));
        assertTrue(html.contains(SecurityEmailHtml.DEFAULT_HEADER_TITLE));
        assertTrue(html.contains("<p>Hi</p>"));
        assertTrue(html.contains("Foot"));
        assertTrue(html.contains(".container"));
    }

    @Test
    void document_withExtraCss_appendsStyles() {
        String html = SecurityEmailHtml.document(".x { color: red; }", false, "<p>x</p>", "");
        assertTrue(html.contains(".x { color: red; }"));
    }

    @Test
    void document_withViewport_includesMeta() {
        String html = SecurityEmailHtml.document("", true, "", "");
        assertTrue(html.contains("viewport"));
    }
}
