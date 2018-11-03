package views;

import extension.HashHelper;
import models.Group;
import models.User;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import play.mvc.Http;
import play.test.Helpers;
import play.test.WithBrowser;
import play.twirl.api.Content;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static play.libs.Scala.asScala;
import static play.test.Helpers.contentAsString;


public class IndexTemplateTests  extends WithBrowser {

    @Before
    public void setup() {
        browser.goTo("/login");
        assertEquals(browser.url(), "login");
        assertThat(browser.$("title").text()).isNotEmpty();
        browser.find(By.xpath("//*[@id=\"username\"]")).write("admin");
        browser.find(By.xpath("//*[@id=\"password\"]")).write("admin");
        browser.find(By.xpath("/html/body/div/div/div/main/div/form/button")).click();
    }

    @Test
    public void renderIndexTemplate() {
        HashHelper hashHelper = new HashHelper();
        User admin = new User("admin", "hsh.helper+admin@gmaisl.com", hashHelper.hashPassword("admin"), false, 10);
        Group all = new Group("All", admin);
        Group admins = new Group("Administrators", admin);

        all.setMembers(Stream.of(admin).collect(Collectors.toSet()));
        admins.setMembers(Stream.of(admin).collect(Collectors.toSet()));
        admin.setGroups(Stream.of(all, admins).collect(Collectors.toSet()));

        Content html = views.html.Index.render(admin, asScala(admin.getGroups()));
        assertEquals("text/html", html.contentType());
        assertTrue(contentAsString(html).contains("<title>HsH-Helfer: Index</title>"));
        assertTrue(contentAsString(html).contains("Bisher ist die einzige Funktionalitaet des HsH-Helfers"));

        Pattern regexAllGroup = Pattern.compile("<a .* class=\"list-group-item list-group-item-action\">All</a>");
        Pattern regexAdminsGroup = Pattern.compile("<a .* class=\"list-group-item list-group-item-action\">Administrators</a>");

        assertTrue(regexAllGroup.matcher(contentAsString(html)).find());
        assertTrue(regexAdminsGroup.matcher(contentAsString(html)).find());
    }
}
