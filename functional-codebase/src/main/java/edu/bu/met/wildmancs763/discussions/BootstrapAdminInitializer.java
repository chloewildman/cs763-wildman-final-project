package edu.bu.met.wildmancs763.discussions;

import edu.bu.met.wildmancs763.discussions.service.UserAccountService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

    private final UserAccountService userAccountService;
    private final String username;
    private final String password;

    public BootstrapAdminInitializer(
            UserAccountService userAccountService,
            @Value("${app.bootstrap-admin.username:}") String username,
            @Value("${app.bootstrap-admin.password:}") String password) {
        this.userAccountService = userAccountService;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean hasUsername = StringUtils.hasText(username);
        boolean hasPassword = StringUtils.hasText(password);
        if (hasUsername != hasPassword) {
            throw new IllegalStateException(
                    "Both app.bootstrap-admin.username and app.bootstrap-admin.password must be configured together.");
        }
        if (hasUsername) {
            userAccountService.createBootstrapAdmin(username, password);
        }
    }
}
