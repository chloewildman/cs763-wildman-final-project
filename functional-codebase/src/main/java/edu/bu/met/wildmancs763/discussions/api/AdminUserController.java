/* 
Architectural Header

Language stack: Java/Spring Boot
AI contribution: 100%
Human verification trails:
- I did not have time to write my own unit tests, but existing tests pass.
- Manual API testing using Postman, see conceptual journal
*/

package edu.bu.met.wildmancs763.discussions.api;

import edu.bu.met.wildmancs763.discussions.api.DiscussionDtos.UserProvisionRequest;
import edu.bu.met.wildmancs763.discussions.api.DiscussionDtos.UserResponse;
import edu.bu.met.wildmancs763.discussions.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final UserAccountService userAccountService;

    public AdminUserController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse provisionUser(@Valid @RequestBody UserProvisionRequest request) {
        return userAccountService.provision(request);
    }
}
