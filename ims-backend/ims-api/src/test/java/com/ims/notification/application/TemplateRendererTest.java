package com.ims.notification.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class TemplateRendererTest {

  private final TemplateRenderer renderer = new TemplateRenderer();

  @Test
  void replacesPlaceholders() {
    String out =
        renderer.render(
            "Hello {{name}}, ref {{ref}}", Map.of("name", "Ada", "ref", "A-1"));
    assertThat(out).isEqualTo("Hello Ada, ref A-1");
  }

  @Test
  void nullValueBecomesEmpty() {
    assertThat(renderer.render("x={{v}}", Map.of("v", ""))).isEqualTo("x=");
  }
}
