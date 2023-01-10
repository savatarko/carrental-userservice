package org.komponente.userservice.domain;

//import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Embeddable;

@Getter
@Setter
@Embeddable
public class LoginInfo {
    private String email;
    private String password;
    private String salt;
}
