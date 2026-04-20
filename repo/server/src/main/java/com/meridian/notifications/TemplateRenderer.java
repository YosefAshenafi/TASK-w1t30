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
        String rendered = render(markdownTemplate, vars);
        Node document = markdownParser.parse(rendered);
        return htmlRenderer.render(document);
    }
}
