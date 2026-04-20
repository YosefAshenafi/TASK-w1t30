package com.meridian.notifications;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TemplateRenderer {

    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");
    private final Parser markdownParser = Parser.builder().build();
    private final HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();

    public String render(String template, Map<String, String> vars) {
        if (template == null) return "";
        StringBuffer sb = new StringBuffer();
        Matcher m = VAR_PATTERN.matcher(template);
        while (m.find()) {
            String key = m.group(1);
            m.appendReplacement(sb, Matcher.quoteReplacement(vars.getOrDefault(key, "")));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public String renderToHtml(String markdownTemplate, Map<String, String> vars) {
        // Variable substitutions are HTML-escaped before the markdown parser
        // sees them so that user-provided content (usernames, course names,
        // free-form notes) cannot inject script tags or attributes into the
        // rendered HTML body of notifications.
        Map<String, String> escaped = escapeValues(vars);
        String rendered = render(markdownTemplate, escaped);
        Node document = markdownParser.parse(rendered);
        return htmlRenderer.render(document);
    }

    private static Map<String, String> escapeValues(Map<String, String> in) {
        if (in == null || in.isEmpty()) return java.util.Map.of();
        java.util.Map<String, String> out = new java.util.HashMap<>(in.size());
        for (var entry : in.entrySet()) {
            out.put(entry.getKey(), escapeHtml(entry.getValue()));
        }
        return out;
    }

    private static String escapeHtml(String v) {
        if (v == null) return "";
        StringBuilder sb = new StringBuilder(v.length());
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
