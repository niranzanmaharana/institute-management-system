package com.ims.notification.application;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TemplateRenderer {

  public String render(String template, Map<String, String> vars) {
    if (template == null) {
      return "";
    }
    String out = template;
    if (vars != null) {
      for (Map.Entry<String, String> e : vars.entrySet()) {
        String key = e.getKey();
        String value = e.getValue() == null ? "" : e.getValue();
        out = out.replace("{{" + key + "}}", value);
      }
    }
    return out;
  }
}
