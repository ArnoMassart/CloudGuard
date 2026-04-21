package com.cloudmen.cloudguard.utility;

/**
 * Shared HTML document shell for CloudGuard transactional emails (card layout, header, footer).
 *
 * <p>Used by {@link com.cloudmen.cloudguard.service.notification.FeedbackEmailService},
 * {@link com.cloudmen.cloudguard.service.report.SecurityReportEmailService},
 * {@link com.cloudmen.cloudguard.service.reminder.CriticalNotificationReminderEmailService}, and
 * {@link com.cloudmen.cloudguard.service.AccessRequestEmailService}.
 */
public final class SecurityEmailHtml {

    public static final String HEADER_BG = "#011624";
    public static final String PRIMARY = "#3abfad";
    public static final String MUTED_BG = "#ececf0";
    public static final String MUTED_TEXT = "#717182";
    public static final String CARD_BG = "#ffffff";
    public static final String FOREGROUND = "#030213";

    /** Shown in the dark header bar of every standard mail. */
    public static final String DEFAULT_HEADER_TITLE = "CloudGuard Security";

    private static final String BASE_CSS = """
            body { margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, sans-serif; background-color: #f3f3f5; }
            .container { max-width: 600px; margin: 20px auto; background: %s; border-radius: 8px; overflow: hidden; border: 1px solid #e5e7eb; }
            .header { background: %s; padding: 24px 32px; text-align: center; color: white; }
            .content { padding: 32px; color: %s; line-height: 1.5; }
            .footer { background: %s; padding: 20px 32px; font-size: 12px; color: %s; text-align: center; }
            """;

    private SecurityEmailHtml() {}

    /** Base stylesheet block (same colors as {@link #document}) for reuse in custom templates if needed. */
    public static String baseCssBlock() {
        return BASE_CSS.formatted(CARD_BG, HEADER_BG, FOREGROUND, MUTED_BG, MUTED_TEXT);
    }

    /**
     * Full HTML5 document with the standard shell.
     *
     * @param extraHeadCss optional CSS appended after the base rules (e.g. {@code .card { ... }})
     * @param includeViewport whether to emit a viewport meta tag (mobile clients)
     * @param contentHtml inner HTML for {@code .content} (escape user-controlled text before passing)
     * @param footerHtml footer copy (usually escaped)
     */
    public static String document(String extraHeadCss, boolean includeViewport, String contentHtml, String footerHtml) {
        String metaViewport =
                includeViewport ? "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" : "";
        String css = baseCssBlock();
        if (extraHeadCss != null && !extraHeadCss.isBlank()) {
            css = css + extraHeadCss;
        }
        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8">
                %s  <style>
                %s
                  </style>
                </head>
                <body>
                  <div class="container">
                    <div class="header">
                      <h2 style="margin: 0;">%s</h2>
                    </div>
                    <div class="content">
                %s
                    </div>
                    <div class="footer">
                      %s
                    </div>
                  </div>
                </body>
                </html>
                """
                .formatted(metaViewport, css, DEFAULT_HEADER_TITLE, contentHtml, footerHtml);
    }

    /** Same as {@link #document(String, boolean, String, String)} with no extra CSS and no viewport meta. */
    public static String document(String contentHtml, String footerHtml) {
        return document("", false, contentHtml, footerHtml);
    }
}
