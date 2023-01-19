package org.komponente.userservice.controller;

import io.swagger.annotations.ApiOperation;
import org.komponente.dto.client.ClientCreateDto;
import org.komponente.dto.client.ClientDto;
import org.komponente.dto.rank.RankCreateDto;
import org.komponente.dto.rank.RankDto;
import org.komponente.dto.user.UserDto;
import org.komponente.userservice.security.CheckSecurity;
import org.komponente.userservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/rank")
public class RankController {

    private UserService userService;

    public RankController(UserService userService) {
        this.userService = userService;
    }

    /*
    public RankController() {
    }

     */

    @PostMapping("/create")
    @CheckSecurity(roles = {"ROLE_ADMIN"})
    public ResponseEntity<RankDto> createRank(@RequestHeader("Authorization") String authorization, @RequestBody @Valid RankCreateDto rankCreateDto){
        return new ResponseEntity<>(userService.add(rankCreateDto), HttpStatus.CREATED);
    }

    @PutMapping("/update/{id}")
    @CheckSecurity(roles = {"ROLE_ADMIN"})
    public ResponseEntity<RankDto> updateRank(@RequestHeader("Authorization") String authorization, @RequestBody @Valid RankCreateDto rankCreateDto, @PathVariable Long id){
        return new ResponseEntity<>(userService.changeRank(id, rankCreateDto), HttpStatus.OK);
    }

    //TODO: check security kako radi sa restom?
    @GetMapping("{id}")
    public ResponseEntity<Long> getRank(@RequestHeader("Authorization") String authorization, @PathVariable Long id){
        return new ResponseEntity<>(userService.getRank(id), HttpStatus.OK);
    }

    @ApiOperation(value = "Get all ranks (admin purposes)")
    @GetMapping("/getall")
    @CheckSecurity(roles = {"ROLE_ADMIN"})
    public ResponseEntity<List<RankDto>> test()
    {
        return new ResponseEntity<>(userService.getAllRanks(), HttpStatus.OK);
    }
}
