package org.komponente.userservice.domain;

//import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Getter
@Setter
//@NoArgsConstructor
//@AllArgsConstructor

@Entity
public class Admin extends User{
}
