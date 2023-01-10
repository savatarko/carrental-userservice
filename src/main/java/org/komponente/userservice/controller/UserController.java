package org.komponente.userservice.controller;

import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.komponente.dto.client.ClientCreateDto;
import org.komponente.dto.client.ClientDto;
import org.komponente.dto.manager.ManagerCreateDto;
import org.komponente.dto.manager.ManagerDto;
import org.komponente.dto.token.TokenRequestDto;
import org.komponente.dto.token.TokenResponseDto;
import org.komponente.dto.user.ChangeUserDto;
import org.komponente.userservice.security.CheckSecurity;
import org.komponente.userservice.security.token.TokenService;
import org.komponente.userservice.service.UserService;
import org.komponente.userservice.service.implementations.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;


@AllArgsConstructor
@RestController
@RequestMapping("/user")
public class UserController {
    private UserService userService;
    private TokenService tokenService;



    /*
    public UserController() {
    }

     */


    @ApiOperation(value = "login")
    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> loginUser(@RequestBody @Valid TokenRequestDto tokenRequestDto) {
        return new ResponseEntity<>(userService.login(tokenRequestDto), HttpStatus.OK);
    }



    @ApiOperation(value = "Register client")
    @PostMapping("/register/client")
    public ResponseEntity<ClientDto> registerClient(@RequestBody @Valid ClientCreateDto clientCreateDto)
    {
        return new ResponseEntity<>(userService.add(clientCreateDto), HttpStatus.CREATED);
        //return null;
    }

    @ApiOperation(value = "Register manager")
    @PostMapping("/register/manager")
    public ResponseEntity<ManagerDto> registerManager(@RequestBody @Valid ManagerCreateDto managerCreateDto)
    {
        return new ResponseEntity<>(userService.add(managerCreateDto), HttpStatus.CREATED);
        //return null;
    }

    @ApiOperation(value = "Revoke rights")
    @PutMapping("/revoke/{id}")
    @CheckSecurity(roles = {"ROLE_ADMIN"})
    public ResponseEntity<?> removeAccess(@RequestHeader("Authorization") String authorization, @PathVariable Long id)
    {
        userService.removeAccess(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = "Return rights")
    @PutMapping("/grant/{id}")
    @CheckSecurity(roles = {"ROLE_ADMIN"})
    public ResponseEntity<?> grantAccess(@RequestHeader("Authorization") String authorization, @PathVariable Long id)
    {
        userService.giveAccess(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation(value = "Update user")
    @PutMapping("/change/{id}")
    @CheckSecurity(roles = {"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_CLIENT"})
    public ResponseEntity<?> changeUser(@RequestHeader("Authorization") String authorization, @PathVariable Long id, @RequestBody ChangeUserDto changeUserDto)
    {
        if(tokenService.parseToken(authorization).get("role", String.class).equals("ROLE_ADMIN")) {
            userService.changeAdmin(id, changeUserDto);
            return new ResponseEntity<>(HttpStatus.OK);
        }
        else if(tokenService.parseToken(authorization).get("role", String.class).equals("ROLE_MANAGER")) {
            userService.changeManager(id, changeUserDto);
            return new ResponseEntity<>(HttpStatus.OK);
        }
        else if(tokenService.parseToken(authorization).get("role", String.class).equals("ROLE_CLIENT")) {
            userService.changeClient(id, changeUserDto);
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
}
