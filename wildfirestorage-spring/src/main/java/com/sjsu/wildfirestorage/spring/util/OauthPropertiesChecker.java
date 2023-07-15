package com.sjsu.wildfirestorage.spring.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * This class will check to make sure that the oauth configuration looks valid and
 * that we aren't getting any oauth information from the class path. (we want to make
 * sure that our secrets don't leak into the source repo.)
 * It will System.exit on any problems.
 */
@Configuration
public class OauthPropertiesChecker implements CommandLineRunner {

    @Autowired
    AbstractEnvironment env;

    @Override
    public void run(String... args) {
        System.out.println("Hello property checker here!");
        var oauthSecrets = new ArrayList<String>();
        var oauthSecretsSources = new ArrayList<PropertySource<?>>();
        var oauthIds = new ArrayList<String>();
        env.getPropertySources().stream().iterator().forEachRemaining(s -> {
            if (s instanceof EnumerablePropertySource<?> es) {
                Arrays.stream(es.getPropertyNames())
                        .filter(n -> n.startsWith("spring.security.oauth2.client.registration")).forEach(n -> {
                            var parts = n.split("\\.");
                            var provider = parts[parts.length - 2];
                            var prop = parts[parts.length - 1];
                            if (prop.equals("clientId")) {oauthIds.add(provider);}
                            if (prop.equals("clientSecret")) {
                                oauthSecrets.add(provider);
                                oauthSecretsSources.add(es);
                            }
                        });
            }
        });
        if (oauthIds.size() == 0) {
            System.out.println("no oauth properties found. properties should have the form");
            System.out.println("  spring.security.oauth2.client.registration.{provider}.clientId and");
            System.out.println("  spring.security.oauth2.client.registration.{provider}.clientSecret");
            System.exit(1);
        }
        for (int i = 0; i < oauthSecretsSources.size(); i++) {
            var s = oauthSecretsSources.get(i);
            if (s.getName().contains("classpath:")) {
                System.out.printf("%s information was found on classpath. this is a security violation: %s",
                        oauthIds.get(i), s);
                System.exit(1);
            }
        }
        if (oauthIds.size() != oauthSecrets.size()) {
            System.out.printf("id list %s doesn't match secret list %s%n", oauthIds, oauthSecrets);
            System.exit(1);
        }
        var duplicates =
                oauthIds.stream().filter(id -> Collections.frequency(oauthIds, id) > 1).collect(Collectors.toSet());
        if (duplicates.size() > 0) {
            System.out.println("duplicate oauthIds found: " + duplicates);
            System.exit(1);
        }
        System.out.println("everything checks out! we are good to go!");
    }
}