package org.komponente.userservice.domain;

//import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

import java.time.LocalDate;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

@MappedSuperclass
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /*
    @Embedded
    private LoginInfo loginInfo;

     */

    private String username;
    private String name;
    private String surname;
    private String number;
    //@Temporal(TemporalType.DATE)
    private LocalDate dateofbirth;
    private Boolean hasaccess;

    private String email;
    private String password;
    private String salt;

    private Long activated;

    private String tempPassword;
    private Long confirmTempPassword;


}
